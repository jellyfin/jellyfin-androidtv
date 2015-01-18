package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;
import mediabrowser.model.users.AuthenticationResult;


public class StartupActivity extends Activity {

    private TvApp application;
    private ILogger logger;
    private Calendar expirationDate = new GregorianCalendar(2015,0,22);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);
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
