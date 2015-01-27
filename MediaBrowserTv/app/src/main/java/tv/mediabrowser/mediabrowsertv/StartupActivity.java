package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.prefs.Preferences;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.connectionmanager.ConnectionManager;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;


public class StartupActivity extends Activity {

    private TvApp application;
    private ILogger logger;
    private Calendar expirationDate = new GregorianCalendar(2015,1,2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);

        //Ensure we have prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        application = (TvApp) getApplicationContext();
        final Activity activity = this;
        logger = application.getLogger();
        logger.Debug("exp: " + expirationDate);
        logger.Debug("now: " + new GregorianCalendar());
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


        ApiEventListener apiEventListener = new ApiEventListener();

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
                                final EditText password = new EditText(activity);
                                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                new AlertDialog.Builder(activity)
                                        .setTitle("Enter Password")
                                        .setMessage("Please enter password for " + response.getName())
                                        .setView(password)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                String pw = password.getText().toString();
                                                Utils.loginUser(response.getName(), pw, application.getLoginApiClient(), activity);
                                            }
                                        }).show();

                            } else {
                                application.setCurrentUser(response);
                                Intent intent = new Intent(activity, MainActivity.class);
                                activity.startActivity(intent);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Utils.reportError(activity, "Error Signing In");
                            connectAutomatically(connectionManager, activity);
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
            public void onResponse(ConnectionResult response) {
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
                        logger.Debug("Already signed in");
                        response.getApiClient().GetUserAsync(response.getApiClient().getCurrentUserId(), new Response<UserDto>() {
                            @Override
                            public void onResponse(UserDto response) {
                                application.setCurrentUser(response);
                                Intent intent = new Intent(activity, MainActivity.class);
                                startActivity(intent);
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
                                startActivity(serverIntent);
                            }
                        });
                        break;
                }
            }
        });

    }


}
