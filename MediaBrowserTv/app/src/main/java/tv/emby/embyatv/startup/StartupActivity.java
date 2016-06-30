package tv.emby.embyatv.startup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.model.apiclient.ConnectionState;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;
import mediabrowser.model.session.GeneralCommandType;
import tv.emby.embyatv.BuildConfig;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.browsing.MainActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.eventhandling.TvApiEventListener;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.util.Utils;


public class StartupActivity extends Activity {

    private static final int NETWORK_PERMISSION = 1;
    private TvApp application;
    private ILogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);


        application = (TvApp) getApplicationContext();
        logger = application.getLogger();

        //Migrate prefs
        if (Integer.parseInt(application.getConfigVersion()) < 2) {
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "2").commit();
        }
        if (Integer.parseInt(application.getConfigVersion()) < 3) {
            application.getPrefs().edit().putString("pref_max_bitrate", "0").apply();
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "3").apply();
        }
        if (Integer.parseInt(application.getConfigVersion()) < 4) {
            application.getPrefs().edit().putBoolean("pref_enable_premieres", false).apply();
            application.getPrefs().edit().putBoolean("pref_enable_info_panel", false).apply();
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "4").apply();
        }

        //Ensure we have prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Ensure basic permissions
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
            logger.Info("Requesting network permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, NETWORK_PERMISSION);
        } else {
            logger.Info("Basic network permissions are granted");
            start();
        }
    }

    private void start() {
        if (application.getCurrentUser() != null && application.getApiClient() != null && MediaManager.isPlayingAudio()) {
            // go straight into last connection
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        } else {
            //clear audio queue in case left over from last run
            MediaManager.clearAudioQueue();
            MediaManager.clearVideoQueue();
            establishConnection(this);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case NETWORK_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    start();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Utils.showToast(this, "Application cannot continue without network");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }

    }

    private void establishConnection(final Activity activity){
        // The underlying http stack. Developers can inject their own if desired
        VolleyHttpClient volleyHttpClient = new VolleyHttpClient(logger, application);
        TvApp.getApplication().setHttpClient(volleyHttpClient);
        ClientCapabilities capabilities = new ClientCapabilities();
        ArrayList<String> playableTypes = new ArrayList<>();
        playableTypes.add("Video");
        playableTypes.add("Audio");
        ArrayList<String> supportedCommands = new ArrayList<>();
        supportedCommands.add(GeneralCommandType.DisplayContent.toString());
        supportedCommands.add(GeneralCommandType.Mute.toString());
        supportedCommands.add(GeneralCommandType.Unmute.toString());
        supportedCommands.add(GeneralCommandType.ToggleMute.toString());

        capabilities.setPlayableMediaTypes(playableTypes);
        capabilities.setSupportsContentUploading(false);
        capabilities.setSupportsSync(false);
        capabilities.setDeviceProfile(new AndroidProfile(Utils.getProfileOptions()));
        capabilities.setSupportsMediaControl(true);
        capabilities.setSupportedCommands(supportedCommands);
        capabilities.setAppStoreUrl(Utils.getStoreUrl());
        capabilities.setIconUrl("https://raw.githubusercontent.com/MediaBrowser/MediaBrowser.Android/master/servericon.png");

        IJsonSerializer jsonSerializer = new GsonJsonSerializer();


        ApiEventListener apiEventListener = new TvApiEventListener();

        final IConnectionManager connectionManager = new AndroidConnectionManager(application,
                jsonSerializer,
                logger,
                volleyHttpClient,
                "AndroidTv",
                BuildConfig.VERSION_NAME,
                new AndroidDevice(application),
                capabilities,
                apiEventListener);

        application.setConnectionManager(connectionManager);
        application.setSerializer((GsonJsonSerializer) jsonSerializer);

        application.setPlaybackManager(new PlaybackManager(new AndroidDevice(application), logger));

        //See if we are coming in via direct entry
        application.setDirectItemId(getIntent().getStringExtra("ItemId"));

        //Load any saved login creds
        application.setConfiguredAutoCredentials(Utils.GetSavedLoginCredentials(application.getDirectItemId() == null ? "tv.mediabrowser.login.json" : "tv.emby.lastlogin.json"));

        //And use those credentials if option is set
        if (application.getIsAutoLoginConfigured() || application.getDirectItemId() != null) {
            //Auto login as configured user - first connect to server
            connectionManager.Connect(application.getConfiguredAutoCredentials().getServerInfo(), new Response<ConnectionResult>() {
                @Override
                public void onResponse(ConnectionResult response) {
                    if (response.getState() == ConnectionState.Unavailable) {
                        Utils.showToast( activity, "Unable to connect to configured server "+application.getConfiguredAutoCredentials().getServerInfo().getName());
                        connectAutomatically(connectionManager, activity);
                        return;
                    }
                    // Connected to server - load user and prompt for pw if necessary
                    application.setLoginApiClient(response.getApiClient());
                    response.getApiClient().GetUserAsync(application.getConfiguredAutoCredentials().getUserDto().getId(), new Response<UserDto>() {
                        @Override
                        public void onResponse(final UserDto response) {
                            application.setCurrentUser(response);
                            if (application.getDirectItemId() != null) {
                                application.validate();
                                application.determineAutoBitrate();
                                if (response.getHasPassword()
                                        && (!application.getIsAutoLoginConfigured()
                                        || (application.getPrefs().getBoolean("pref_auto_pw_prompt", false)))) {
                                    //Need to prompt for pw
                                    Utils.processPasswordEntry(activity, response, application.getDirectItemId());
                                } else {
                                    //Can just go right into details

                                    Intent detailsIntent = new Intent(activity, FullDetailsActivity.class);
                                    detailsIntent.putExtra("ItemId", application.getDirectItemId());
                                    startActivity(detailsIntent);
                                }

                            } else {
                                if (response.getHasPassword() && application.getPrefs().getBoolean("pref_auto_pw_prompt", false)) {
                                    Utils.processPasswordEntry(activity, response);

                                } else {
                                    Intent intent = new Intent(activity, MainActivity.class);
                                    activity.startActivity(intent);
                                }
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            application.getLogger().ErrorException("Error Signing in", exception);
                            Utils.reportError(activity, "Error Signing In");
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectAutomatically(connectionManager, activity);
                                }
                            }, 5000);
                        }
                    });
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast( activity, "Unable to connect to configured server "+application.getConfiguredAutoCredentials().getServerInfo().getName());
                    connectAutomatically(connectionManager, activity);
                }
            });
        } else {
            connectAutomatically(connectionManager, activity);
        }
    }

    private void connectAutomatically(final IConnectionManager connectionManager, final Activity activity){
        connectionManager.Connect(new Response<ConnectionResult>() {
            @Override
            public void onResponse(final ConnectionResult response) {
                Utils.handleConnectionResponse(connectionManager, activity, response);
            }

            @Override
            public void onError(Exception exception) {
                Utils.reportError(activity, "Error connecting");
            }
        });

    }


}
