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
import org.jellyfin.androidtv.model.LogonCredentials;
import org.jellyfin.androidtv.model.repository.SerializerRepository;
import org.jellyfin.androidtv.startup.SelectUserActivity;
import org.jellyfin.androidtv.util.DelayedMessage;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.ConnectionResult;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.apiclient.ServerInfo;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.users.AuthenticationResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

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
                Timber.d("Entered address: %s", addressValue);
                TvApp.getApplication().getApiClient().ChangeServerLocation(addressValue);
                AuthenticationHelper.enterManualUser(activity);
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
                Timber.d("Entered user: %s", userValue);
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
                        loginUser(userName.getText().toString(), userPw.getText().toString(), TvApp.getApplication().getApiClient(), activity);
                    }
                }).show();
            }
        }).show();
    }

    private static Response<ConnectionResult> getSignInResponse(final Activity activity, final String address) {
        // This is taking longer than expected message
        final DelayedMessage message = new DelayedMessage(activity);

        return new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                message.Cancel();

                // Check the server version
                if (!serverResult.getServers().isEmpty() &&
                        !isSupportedServerVersion(serverResult.getServers().get(0))) {
                    Utils.showToast(activity, activity.getString(R.string.msg_error_server_version, TvApp.MINIMUM_SERVER_VERSION));
                    return;
                }

                switch (serverResult.getState()) {
                    case Unavailable:
                        Utils.showToast(activity, R.string.msg_error_server_unavailable);
                        break;
                    case SignedIn:
                    case ServerSignIn:
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    default:
                        Timber.e("Unexpected response %s trying to sign in to specific server %s", serverResult.getState().toString(), address);
                        Utils.showToast(activity, activity.getString(R.string.msg_error_connecting_server));
                }
            }

            @Override
            public void onError(Exception exception) {
                message.Cancel();
                Timber.e(exception, "Error trying to sign in to specific server %s", address);
                Utils.showToast(activity, activity.getString(R.string.msg_error_connecting_server));
            }
        };
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity) {
        loginUser(userName, pw, apiClient, activity, null);
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity, final String directEntryItemId) {
        apiClient.AuthenticateUserAsync(userName, pw, new Response<AuthenticationResult>() {
            @Override
            public void onResponse(AuthenticationResult authenticationResult) {
                TvApp application = TvApp.getApplication();
                Timber.d("Signed in as %s", authenticationResult.getUser().getName());
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
                Timber.e(exception, "Error logging in");
                Utils.showToast(activity, activity.getString(R.string.msg_invalid_id_pw));
            }
        });
    }

    public static void saveLoginCredentials(LogonCredentials creds, String fileName) throws IOException {
        TvApp app = TvApp.getApplication();
        OutputStream credsFile = app.openFileOutput(fileName, Context.MODE_PRIVATE);
        credsFile.write(SerializerRepository.INSTANCE.getSerializer().SerializeToString(creds).getBytes());
        credsFile.close();
        app.setConfiguredAutoCredentials(creds);
    }

    public static LogonCredentials getSavedLoginCredentials(String fileName) {
        TvApp app = TvApp.getApplication();
        try {
            InputStream credsFile = app.openFileInput(fileName);
            String json = Utils.readStringFromStream(credsFile);
            credsFile.close();
            Timber.d("Saved credential JSON: %s", json);
            return SerializerRepository.INSTANCE.getSerializer().DeserializeFromString(json, LogonCredentials.class);
        } catch (IOException e) {
            Timber.e(e, "IO Error while saving login");
            // none saved
            return new LogonCredentials(new ServerInfo(), new UserDto());
        } catch (Exception e) {
            Timber.e(e, "Error interpreting saved login");
            return new LogonCredentials(new ServerInfo(), new UserDto());
        }
    }

    /**
     * Check if the server version is supported by the app.
     *
     * @param serverInfo The ServerInfo returned by a Connect API call
     * @return true if the server version is supported or not specified
     */
    public static boolean isSupportedServerVersion(final ServerInfo serverInfo) {
        if (serverInfo != null && serverInfo.getVersion() != null) {
            return Utils.versionGreaterThanOrEqual(serverInfo.getVersion(), TvApp.MINIMUM_SERVER_VERSION);
        }
        // If a version is not available, allow the server
        return true;
    }
}
