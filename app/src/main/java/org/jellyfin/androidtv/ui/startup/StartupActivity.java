package org.jellyfin.androidtv.ui.startup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.browsing.MainActivity;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.playback.MediaManager;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class StartupActivity extends FragmentActivity {
    private static final int NETWORK_PERMISSION = 1;
    private TvApp application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_startup);

        application = (TvApp) getApplicationContext();

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
        if (application.getCurrentUser() != null && MediaManager.isPlayingAudio()) {
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
                get(ApiClient.class).GetItemAsync(itemId, get(ApiClient.class).getCurrentUserId(), new Response<BaseItemDto>() {
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
        //See if we are coming in via direct entry
        application.setDirectItemId(getIntent().getStringExtra("ItemId"));

        // Ask for server information
        AuthenticationHelper.enterManualServerAddress(this);
    }
}
