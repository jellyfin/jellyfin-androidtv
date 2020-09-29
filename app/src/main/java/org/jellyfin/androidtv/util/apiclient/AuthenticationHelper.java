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
import org.jellyfin.androidtv.data.model.LogonCredentials;
import org.jellyfin.androidtv.ui.browsing.MainActivity;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.shared.KeyboardFocusChangeListener;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.apiclient.ServerInfo;
import org.jellyfin.apiclient.model.users.AuthenticationResult;
import org.jellyfin.apiclient.serialization.GsonJsonSerializer;

import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class AuthenticationHelper {
    public static void enterManualServerAddress(final Activity activity) {
        final EditText address = new EditText(activity);
        address.setHint(activity.getString(R.string.lbl_ip_hint));
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setOnFocusChangeListener(new KeyboardFocusChangeListener());
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
                ServerInfo info = new ServerInfo();
                info.setAddress(addressValue);
                get(ApiClient.class).EnableAutomaticNetworking(info);
                AuthenticationHelper.enterManualUser(activity);
            }
        }).show();
    }

    public static void enterManualUser(final Activity activity) {
        final EditText userName = new EditText(activity);
        userName.setInputType(InputType.TYPE_CLASS_TEXT);
        userName.setOnFocusChangeListener(new KeyboardFocusChangeListener());
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
                userPw.setOnFocusChangeListener(new KeyboardFocusChangeListener());
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.lbl_enter_user_pw))
                        .setView(userPw)
                        .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        loginUser(userName.getText().toString(), userPw.getText().toString(), get(ApiClient.class), activity);
                    }
                }).show();
            }
        }).show();
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
        credsFile.write(get(GsonJsonSerializer.class).SerializeToString(creds).getBytes());
        credsFile.close();
        app.setConfiguredAutoCredentials(creds);
    }
}
