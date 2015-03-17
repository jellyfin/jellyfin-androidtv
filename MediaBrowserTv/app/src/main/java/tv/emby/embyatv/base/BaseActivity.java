package tv.emby.embyatv.base;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/18/2015.
 */
public class BaseActivity extends Activity {

    private TvApp app = TvApp.getApplication();
    private long timeoutInterval = 3600000;
    private Handler autoLogoutHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeoutInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(app).getString("pref_auto_logoff_timeout","3600000"));
        startAutoLogoffLoop();

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        app.setLastUserInteraction(System.currentTimeMillis());
    }

    private void startAutoLogoffLoop() {
        final Runnable loop = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() > app.getLastUserInteraction() + timeoutInterval) {
                    app.getLogger().Info("Logging off due to inactivity "+app.getLastUserInteraction());
                    Utils.showToast(app, "Emby Logging off due to inactivity...");
                    if (app.getPlaybackController() != null && app.getPlaybackController().isPaused()) {
                        app.getLogger().Info("Playback was paused, stopping gracefully...");
                        app.getPlaybackController().stop();
                    }
                    finish();
                } else {
                    autoLogoutHandler.postDelayed(this, 30000);
                }
            }
        };

        autoLogoutHandler.postDelayed(loop, 60000);

    }

}
