package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;
import mediabrowser.model.users.AuthenticationResult;


public class StartupActivity extends Activity {

    private TvApp application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);
        application = (TvApp) getApplicationContext();
        final Activity activity = this;

        connectToServer(activity);

    }

    private void connectToServer(final Activity activity){
        final ILogger logger = application.getLogger();
        // The underlying http stack. Developers can inject their own if desired
        VolleyHttpClient volleyHttpClient = new VolleyHttpClient(logger, application);
        ClientCapabilities capabilities = new ClientCapabilities();
        IJsonSerializer jsonSerializer = new GsonJsonSerializer();


        ApiEventListener apiEventListener = new ApiEventListener();

        final IConnectionManager connectionManager = new AndroidConnectionManager(application,
                jsonSerializer,
                logger,
                volleyHttpClient,
                "MediaBrowserTv",
                BuildConfig.VERSION_NAME,
                capabilities,
                apiEventListener);

        application.setConnectionManager(connectionManager);
        application.setSerializer(jsonSerializer);

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
                        connectionManager.Connect(response.getServers().get(0), new Response<ConnectionResult>() {
                            @Override
                            public void onResponse(ConnectionResult serverResult) {
                                switch (serverResult.getState()) {
                                    case ServerSignIn:
                                        try {
                                            serverResult.getApiClient().AuthenticateUserAsync("Dad","124", new Response<AuthenticationResult>() {
                                                @Override
                                                public void onResponse(AuthenticationResult authenticationResult) {
                                                    logger.Debug("Signed in as Dad.");
                                                    application.setCurrentUser(authenticationResult.getUser());
                                                    Intent intent = new Intent(activity, MainActivity.class);
                                                    startActivity(intent);
                                                }

                                                @Override
                                                public void onError(Exception exception) {
                                                    super.onError(exception);
                                                    logger.ErrorException("Error logging in", exception);
                                                    Utils.showToast(activity, "Error logging in");
                                                    System.exit(1);
                                                }
                                            });
                                        } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                }
                            }


                    });
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

                }
            }
        });

    }
}
