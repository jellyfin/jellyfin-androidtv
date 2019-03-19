package org.jellyfin.androidtv.util.apiclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.MainActivity;
import org.jellyfin.androidtv.details.FullDetailsActivity;
import org.jellyfin.androidtv.startup.LogonCredentials;
import org.jellyfin.androidtv.startup.SelectServerActivity;
import org.jellyfin.androidtv.startup.SelectUserActivity;
import org.jellyfin.androidtv.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.users.AuthenticationResult;

public class AuthenticationHelper {
    public static void enterManualServerAddress(final Activity activity) {
        final EditText address = new EditText(activity);
        address.setHint(activity.getString(R.string.lbl_ip_hint));
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lbl_enter_server_address))
                .setMessage(activity.getString(R.string.lbl_valid_server_address))
                .setView(address)
                .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String addressValue = address.getText().toString();
                TvApp.getApplication().getLogger().Debug("Entered address: " + addressValue);
                signInToServer(TvApp.getApplication().getConnectionManager(), addressValue.indexOf(":") < 0 ? addressValue + ":8096" : addressValue, activity);
            }
        }).show();
    }

    public static void enterManualUser(final Activity activity) {
        final EditText userName = new EditText(activity);
        userName.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lbl_enter_user_name))
                .setView(userName)
                .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String userValue = userName.getText().toString();
                TvApp.getApplication().getLogger().Debug("Entered user: " + userValue);
                final EditText userPw = new EditText(activity);
                userPw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.lbl_enter_user_pw))
                        .setView(userPw)
                        .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        loginUser(userName.getText().toString(), userPw.getText().toString(), TvApp.getApplication().getLoginApiClient(), activity);
                    }
                }).show();
            }
        }).show();
    }

    public static void signInToServer(IConnectionManager connectionManager, final ServerInfo server, final Activity activity) {
        connectionManager.Connect(server, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                switch (serverResult.getState()) {
                    case SignedIn:
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(serverResult.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    default:
                        TvApp.getApplication().getLogger().Error("Unexpected response " + serverResult.getState() + " trying to sign in to specific server " + server.getLocalAddress());
                        break;
                }
            }
        });
    }

    public static void signInToServer(IConnectionManager connectionManager, String address, final Activity activity) {
        connectionManager.Connect(address, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                switch (serverResult.getState()) {
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(serverResult.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    default:
                        TvApp.getApplication().getLogger().Error("Unexpected response from server login "+ serverResult.getState());
                        Utils.showToast(activity, activity.getString(R.string.msg_error_connecting_server));
                }
            }

            @Override
            public void onError(Exception exception) {
                Utils.reportError(activity, activity.getString(R.string.msg_error_connecting_server));
            }
        });
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity) {
        loginUser(userName, pw, apiClient, activity, null);
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity, final String directEntryItemId) {
        try {
            apiClient.AuthenticateUserAsync(userName, pw, new Response<AuthenticationResult>() {
                @Override
                public void onResponse(AuthenticationResult authenticationResult) {
                    TvApp application = TvApp.getApplication();
                    application.getLogger().Debug("Signed in as " + authenticationResult.getUser().getName());
                    application.setCurrentUser(authenticationResult.getUser());
                    if (directEntryItemId == null) {
                        Intent intent = new Intent(activity, MainActivity.class);
                        activity.startActivity(intent);
                    } else {
                        Intent intent = new Intent(activity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", directEntryItemId);
                        activity.startActivity(intent);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    super.onError(exception);
                    TvApp.getApplication().getLogger().ErrorException("Error logging in", exception);
                    Utils.showToast(activity, activity.getString(R.string.msg_invalid_id_pw));
                }
            });
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void saveLoginCredentials(LogonCredentials creds, String fileName) throws IOException {
        TvApp app = TvApp.getApplication();
        OutputStream credsFile = app.openFileOutput(fileName, Context.MODE_PRIVATE);
        credsFile.write(app.getSerializer().SerializeToString(creds).getBytes());
        credsFile.close();
        app.setConfiguredAutoCredentials(creds);
    }

    public static LogonCredentials getSavedLoginCredentials(String fileName){
        TvApp app = TvApp.getApplication();
        try {
            InputStream credsFile = app.openFileInput(fileName);
            String json = Utils.readStringFromStream(credsFile);
            credsFile.close();
            return (LogonCredentials) app.getSerializer().DeserializeFromString(json, LogonCredentials.class);
        } catch (IOException e) {
            // none saved
            return new LogonCredentials(new ServerInfo(), new UserDto());
        } catch (Exception e) {
            app.getLogger().ErrorException("Error interpreting saved login",e);
            return new LogonCredentials(new ServerInfo(), new UserDto());
        }
    }

    public static void handleConnectionResponse(final IConnectionManager connectionManager,  final Activity activity, ConnectionResult response) {
        ILogger logger = TvApp.getApplication().getLogger();
        switch (response.getState()) {
            case Unavailable:
                logger.Debug("No server available...");
                Utils.showToast(activity, "No Jellyfin Servers available...");
                break;
            case ServerSignIn:
                logger.Debug("Sign in with server " + response.getServers().get(0).getName() + " total: " + response.getServers().size());
                signInToServer(connectionManager, response.getServers().get(0), activity);
                break;
            case SignedIn:
                ServerInfo serverInfo = response.getServers() != null && response.getServers().size() > 0 && response.getServers().get(0).getUserLinkType() != null ? response.getServers().get(0) : null;
                if (serverInfo != null) {
                    // go straight in for connect only
                    response.getApiClient().GetUserAsync(serverInfo.getUserId(), new Response<UserDto>() {
                        @Override
                        public void onResponse(UserDto response) {
                            TvApp.getApplication().setCurrentUser(response);
                            Intent homeIntent = new Intent(activity, MainActivity.class);
                            activity.startActivity(homeIntent);
                        }
                    });

                } else {
                    logger.Debug("Ignoring saved connection manager sign in");
                    connectionManager.GetAvailableServers(new Response<ArrayList<ServerInfo>>(){
                        @Override
                        public void onResponse(ArrayList<ServerInfo> serverResponse) {
                            if (serverResponse.size() == 1) {
                                //Signed in before and have just one server so go directly to user screen
                                signInToServer(connectionManager, serverResponse.get(0), activity);
                            } else {
                                //More than one server so show selection
                                Intent serverIntent = new Intent(activity, SelectServerActivity.class);
                                GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                                List<String> payload = new ArrayList<>();
                                for (ServerInfo server : serverResponse) {
                                    payload.add(serializer.SerializeToString(server));
                                }
                                serverIntent.putExtra("Servers", payload.toArray(new String[] {}));
                                serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                activity.startActivity(serverIntent);
                            }
                        }
                    });

                }
                break;
            case ConnectSignIn:
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
                        serverIntent.putExtra("Servers", payload.toArray(new String[] {}));
                        serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(serverIntent);
                    }
                });
                break;
        }

    }
}
