package org.jellyfin.androidtv.startup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.MainActivity;
import org.jellyfin.androidtv.details.FullDetailsActivity;
import org.jellyfin.androidtv.eventhandling.TvApiEventListener;
import org.jellyfin.androidtv.model.compat.AndroidProfile;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.playback.PlaybackManager;
import org.jellyfin.androidtv.util.ProfileHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;

import java.util.ArrayList;

import org.jellyfin.apiclient.interaction.ApiEventListener;
import org.jellyfin.apiclient.interaction.ConnectionResult;
import org.jellyfin.apiclient.interaction.IConnectionManager;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.interaction.AndroidConnectionManager;
import org.jellyfin.apiclient.interaction.AndroidDevice;
import org.jellyfin.apiclient.interaction.VolleyHttpClient;
import org.jellyfin.apiclient.model.apiclient.ConnectionState;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.logging.ILogger;
import org.jellyfin.apiclient.model.serialization.GsonJsonSerializer;
import org.jellyfin.apiclient.model.session.ClientCapabilities;
import org.jellyfin.apiclient.model.session.GeneralCommandType;

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
        capabilities.setDeviceProfile(new AndroidProfile(ProfileHelper.getProfileOptions()));
        capabilities.setSupportsMediaControl(true);
        capabilities.setSupportedCommands(supportedCommands);

        GsonJsonSerializer jsonSerializer = new GsonJsonSerializer();
        ApiEventListener apiEventListener = new TvApiEventListener();

        final IConnectionManager connectionManager = new AndroidConnectionManager(application,
                jsonSerializer,
                logger,
                volleyHttpClient,
                "AndroidTV",
                BuildConfig.VERSION_NAME,
                new AndroidDevice(application),
                capabilities,
                apiEventListener);

        application.setConnectionManager(connectionManager);
        application.setSerializer(jsonSerializer);
        application.setPlaybackManager(new PlaybackManager(new AndroidDevice(application), logger));

        //See if we are coming in via direct entry
        application.setDirectItemId(getIntent().getStringExtra("ItemId"));

        //Load any saved login creds
        application.setConfiguredAutoCredentials(AuthenticationHelper.getSavedLoginCredentials(TvApp.CREDENTIALS_PATH));

        //And use those credentials if option is set
        if (application.getIsAutoLoginConfigured() || application.getDirectItemId() != null) {
            //Auto login as configured user - first connect to server
            connectionManager.Connect(application.getConfiguredAutoCredentials().getServerInfo(), new Response<ConnectionResult>() {
                @Override
                public void onResponse(ConnectionResult response) {
                    // Saved server login is unavailable
                    if (response.getState() == ConnectionState.Unavailable) {
                        Utils.showToast(activity, R.string.msg_error_server_unavailable + ": " + application.getConfiguredAutoCredentials().getServerInfo().getName());
                        AuthenticationHelper.automaticSignIn(connectionManager, activity);
                        return;
                    }

                    // Check the server version
                    if (!response.getServers().isEmpty() &&
                            !AuthenticationHelper.isSupportedServerVersion(response.getServers().get(0))) {
                        Utils.showToast(activity, activity.getString(R.string.msg_error_server_version, TvApp.MINIMUM_SERVER_VERSION));
                        AuthenticationHelper.automaticSignIn(connectionManager, activity);
                        return;
                    }

                    // Connected to server - load user and prompt for pw if necessary
                    application.setLoginApiClient(response.getApiClient());
                    response.getApiClient().GetUserAsync(application.getConfiguredAutoCredentials().getUserDto().getId(), new Response<UserDto>() {
                        @Override
                        public void onResponse(final UserDto response) {
                            application.setCurrentUser(response);
                            if (application.getDirectItemId() != null) {
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
                            Utils.showToast(activity, R.string.msg_error_signin);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    AuthenticationHelper.automaticSignIn(connectionManager, activity);
                                }
                            }, 5000);
                        }
                    });
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast( activity, R.string.msg_error_connecting_server + ": " + application.getConfiguredAutoCredentials().getServerInfo().getName());
                    AuthenticationHelper.automaticSignIn(connectionManager, activity);
                }
            });
        } else {
            AuthenticationHelper.automaticSignIn(connectionManager, activity);
        }
    }
}
