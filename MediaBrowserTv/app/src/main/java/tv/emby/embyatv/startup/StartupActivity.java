package tv.emby.embyatv.startup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.acra.ACRA;

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

    private TvApp application;
    private ILogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);


        application = (TvApp) getApplicationContext();
        final Activity activity = this;
        logger = application.getLogger();

        //Migrate prefs
        if (Integer.parseInt(application.getConfigVersion()) < 2) {
            application.getPrefs().edit().putString("pref_vlc_max_res", "2900").commit();
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "2").commit();
        }
        if (Integer.parseInt(application.getConfigVersion()) < 3) {
            application.getPrefs().edit().putString("pref_max_bitrate", "0").commit();
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "3").commit();
        }
        if (Integer.parseInt(application.getConfigVersion()) < 5) {
            application.getPrefs().edit().putString("pref_net_buffer", "0.6").commit();
            application.getSystemPrefs().edit().putString("sys_pref_config_version", "5").commit();
        }

        //Ensure we have prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (application.getCurrentUser() != null && application.getApiClient() != null && MediaManager.isPlayingAudio()) {
            // go straight into last connection
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);

        } else {
            //clear audio queue in case left over from last run
            MediaManager.clearAudioQueue();
            establishConnection(activity);
        }

    }

    private void establishConnection(final Activity activity){
        // The underlying http stack. Developers can inject their own if desired
        VolleyHttpClient volleyHttpClient = new VolleyHttpClient(logger, application);
        TvApp.getApplication().setHttpClient(volleyHttpClient);
        ClientCapabilities capabilities = new ClientCapabilities();
        ArrayList<String> playableTypes = new ArrayList<>();
        playableTypes.add("Video");
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
                            ACRA.getErrorReporter().putCustomData("SavedInfo", application.getSerializer().SerializeToString(application.getConfiguredAutoCredentials()));
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
                if (response.getConnectUser() != null) TvApp.getApplication().setConnectLogin(true);
                switch (response.getState()) {
                    case ConnectSignIn:
                        logger.Debug("Sign in with connect...");
                        Intent intent = new Intent(activity, ConnectActivity.class);
                        startActivity(intent);
                        break;

                    case Unavailable:
                        logger.Debug("No server available...");
                        Utils.showToast(application, "No MB Servers available...");
                        break;
                    case ServerSignIn:
                        logger.Debug("Sign in with server "+ response.getServers().get(0).getName() + " total: " + response.getServers().size());
                        Utils.signInToServer(connectionManager, response.getServers().get(0), activity);
                        break;
                    case SignedIn:
                        logger.Debug("Ignoring saved connection manager sign in");
                        connectionManager.GetAvailableServers(new Response<ArrayList<ServerInfo>>(){
                            @Override
                            public void onResponse(ArrayList<ServerInfo> serverResponse) {
                                if (serverResponse.size() == 1) {
                                    //Signed in before and have just one server so go directly to user screen
                                    Utils.signInToServer(connectionManager, serverResponse.get(0), activity);
                                } else {
                                    //More than one server so show selection
                                    Intent serverIntent = new Intent(activity, SelectServerActivity.class);
                                    GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                                    List<String> payload = new ArrayList<>();
                                    for (ServerInfo server : serverResponse) {
                                        payload.add(serializer.SerializeToString(server));
                                    }
                                    serverIntent.putExtra("Servers", payload.toArray(new String[payload.size()]));
                                    serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    startActivity(serverIntent);
                                }
                            }
                        });
                        break;
                    case ServerSelection:
                        logger.Debug("Select A server");
                        connectionManager.GetAvailableServers(new Response<ArrayList<ServerInfo>>(){
                            @Override
                            public void onResponse(ArrayList<ServerInfo> serverResponse) {
                                Intent serverIntent = new Intent(activity, SelectServerActivity.class);
                                GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                                List<String> payload = new ArrayList<>();
                                for (ServerInfo server : serverResponse) {
                                    payload.add(serializer.SerializeToString(server));
                                }
                                serverIntent.putExtra("Servers", payload.toArray(new String[payload.size()]));
                                serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                startActivity(serverIntent);
                            }
                        });
                        break;
                }
            }
        });

    }


}
