package org.jellyfin.androidtv.startup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.browsing.MainActivity;
import org.jellyfin.androidtv.details.FullDetailsActivity;
import org.jellyfin.androidtv.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.model.repository.ConnectionManagerRepository;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.interaction.ConnectionResult;
import org.jellyfin.apiclient.interaction.IConnectionManager;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.apiclient.ConnectionState;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.UserDto;

import timber.log.Timber;

public class StartupActivity extends FragmentActivity {
    private static final int NETWORK_PERMISSION = 1;
    private TvApp application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);

        application = (TvApp) getApplicationContext();

        //Ensure we have prefs
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Ensure basic permissions
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
            Timber.i("Requesting network permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, NETWORK_PERMISSION);
        } else {
            Timber.i("Basic network permissions are granted");
            start();
        }
    }

    private void start() {
        if (application.getCurrentUser() != null && application.getApiClient() != null && MediaManager.isPlayingAudio()) {
            openNextActivity();
        } else {
            //clear audio queue in case left over from last run
            MediaManager.clearAudioQueue();
            MediaManager.clearVideoQueue();
            establishConnection();
        }
    }

    private void openNextActivity() {
        // workaround...
        Activity self = this;
        String itemId = getIntent().getStringExtra("ItemId");
        boolean itemIsUserView = getIntent().getBooleanExtra("ItemIsUserView", false);

        if (itemId != null) {
            if (itemIsUserView) {
                application.getApiClient().GetItemAsync(itemId, application.getApiClient().getCurrentUserId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto item) {
                        ItemLauncher.launchUserView(item, self, true);
                    }

                    @Override
                    public void onError(Exception exception) {
                        // go straight into last connection
                        Intent intent = new Intent(application, MainActivity.class);
                        startActivity(intent);

                        finish();
                    }
                });
            } else {
                //Can just go right into details
                Intent detailsIntent = new Intent(this, FullDetailsActivity.class);
                detailsIntent.putExtra("ItemId", application.getDirectItemId());
                startActivity(detailsIntent);

                finish();
            }
        } else {
            // go straight into last connection
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == NETWORK_PERMISSION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                start();
            } else {
                // permission denied! Disable the app.
                Utils.showToast(this, "Application cannot continue without network");
                finish();
            }
        }
    }

    private void establishConnection() {
        // workaround...
        Activity self = this;

        //See if we are coming in via direct entry
        application.setDirectItemId(getIntent().getStringExtra("ItemId"));

        //Load any saved login creds
        application.setConfiguredAutoCredentials(AuthenticationHelper.getSavedLoginCredentials(TvApp.CREDENTIALS_PATH));

        final IConnectionManager connectionManager = ConnectionManagerRepository.Companion.getInstance(this).getConnectionManager();

        //And use those credentials if option is set
        if (application.getIsAutoLoginConfigured() || application.getDirectItemId() != null) {
            //Auto login as configured user - first connect to server
            connectionManager.Connect(application.getConfiguredAutoCredentials().getServerInfo(), new Response<ConnectionResult>() {
                @Override
                public void onResponse(ConnectionResult response) {
                    // Saved server login is unavailable
                    if (response.getState() == ConnectionState.Unavailable) {
                        Utils.showToast(self, R.string.msg_error_server_unavailable + ": " + application.getConfiguredAutoCredentials().getServerInfo().getName());
                        AuthenticationHelper.automaticSignIn(connectionManager, self);
                        return;
                    }

                    // Check the server version
                    if (!response.getServers().isEmpty() &&
                            !AuthenticationHelper.isSupportedServerVersion(response.getServers().get(0))) {
                        Utils.showToast(self, getString(R.string.msg_error_server_version, TvApp.MINIMUM_SERVER_VERSION));
                        AuthenticationHelper.automaticSignIn(connectionManager, self);
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
                                        || (application.getUserPreferences().getPasswordPromptEnabled()))) {
                                    //Need to prompt for pw
                                    Utils.processPasswordEntry(self, response, application.getDirectItemId());
                                } else {
                                    openNextActivity();
                                }
                            } else {
                                if (response.getHasPassword() && application.getUserPreferences().getPasswordPromptEnabled()) {
                                    Utils.processPasswordEntry(self, response);
                                } else {
                                    openNextActivity();
                                }
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            Timber.e(exception, "Error Signing in");
                            Utils.showToast(self, R.string.msg_error_signin);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    AuthenticationHelper.automaticSignIn(connectionManager, self);
                                }
                            }, 5000);
                        }
                    });
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast(self, R.string.msg_error_connecting_server + ": " + application.getConfiguredAutoCredentials().getServerInfo().getName());
                    AuthenticationHelper.automaticSignIn(connectionManager, self);
                }
            });
        } else {
            AuthenticationHelper.automaticSignIn(connectionManager, self);
        }
    }
}
