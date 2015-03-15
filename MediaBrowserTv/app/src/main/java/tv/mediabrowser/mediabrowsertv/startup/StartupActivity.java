package tv.mediabrowser.mediabrowsertv.startup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;
import tv.mediabrowser.mediabrowsertv.BuildConfig;
import tv.mediabrowser.mediabrowsertv.browsing.MainActivity;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.eventhandling.TvApiEventListener;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.integration.RecommendationManager;
import tv.mediabrowser.mediabrowsertv.itemhandling.ItemLauncher;
import tv.mediabrowser.mediabrowsertv.util.Utils;


public class StartupActivity extends Activity {

    private TvApp application;
    private ILogger logger;
    private Calendar expirationDate = new GregorianCalendar(2015,2,22);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);

        //Ensure we have prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        application = (TvApp) getApplicationContext();
        final Activity activity = this;
        logger = application.getLogger();

        if (new GregorianCalendar().after(expirationDate)) {
            //Expired
            Intent expired = new Intent(this, ExpiredActivity.class);
            expired.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(expired);
        } else {
            establishConnection(activity);
        }

    }

    private void establishConnection(final Activity activity){
        // The underlying http stack. Developers can inject their own if desired
        VolleyHttpClient volleyHttpClient = new VolleyHttpClient(logger, application);
        ClientCapabilities capabilities = new ClientCapabilities();
        IJsonSerializer jsonSerializer = new GsonJsonSerializer();


        ApiEventListener apiEventListener = new TvApiEventListener();

        final IConnectionManager connectionManager = new AndroidConnectionManager(application,
                jsonSerializer,
                logger,
                volleyHttpClient,
                "AndroidTv",
                BuildConfig.VERSION_NAME,
                capabilities,
                apiEventListener);

        application.setConnectionManager(connectionManager);
        application.setSerializer((GsonJsonSerializer)jsonSerializer);

        application.setPlaybackManager(new PlaybackManager(new AndroidDevice(application), logger));

        //Load any saved login creds
        application.setConfiguredAutoCredentials(Utils.GetSavedLoginCredentials());

        //And use those credentials if option is set
        if (application.getIsAutoLoginConfigured()) {
            //Auto login as configured user - first connect to server
            connectionManager.Connect(application.getConfiguredAutoCredentials().getServerInfo(), new Response<ConnectionResult>() {
                @Override
                public void onResponse(ConnectionResult response) {
                    // Connected to server - load user and prompt for pw if necessary
                    application.setLoginApiClient(response.getApiClient());
                    response.getApiClient().GetUserAsync(application.getConfiguredAutoCredentials().getUserDto().getId(), new Response<UserDto>() {
                        @Override
                        public void onResponse(final UserDto response) {
                            if (response.getHasPassword() && application.getPrefs().getBoolean("pref_auto_pw_prompt", false)) {
                                Intent pwIntent = new Intent(activity, DpadPwActivity.class);
                                pwIntent.putExtra("User", application.getSerializer().SerializeToString(response));
                                pwIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                activity.startActivity(pwIntent);

                            } else {
                                application.setCurrentUser(response);
                                Intent intent = new Intent(activity, MainActivity.class);
                                activity.startActivity(intent);
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
