package tv.emby.embyatv.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.search.SearchActivity;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/18/2015.
 */
public class BaseActivity extends Activity {

    private TvApp app = TvApp.getApplication();
    private long timeoutInterval = 3600000;
    private Handler autoLogoutHandler = new Handler();
    private Runnable loop;
    private IKeyListener keyListener;
    private IMessageListener messageListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeoutInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(app).getString("pref_auto_logoff_timeout","3600000"));
        startAutoLogoffLoop();
        TvApp.getApplication().setCurrentActivity(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        TvApp.getApplication().setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TvApp.getApplication().setCurrentActivity(null);
    }

    @Override
    protected void onDestroy() {
        if (autoLogoutHandler != null && loop != null) autoLogoutHandler.removeCallbacks(loop);
        super.onDestroy();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        app.setLastUserInteraction(System.currentTimeMillis());
    }

    private void startAutoLogoffLoop() {
        loop = new Runnable() {
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

    public void registerKeyListener(IKeyListener listener) {
        keyListener = listener;
    }

    public void registerMessageListener(IMessageListener listener) {
        messageListener = listener;
    }

    public void sendMessage(CustomMessage message) {
        if (messageListener != null) messageListener.onMessageReceived(message);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyListener != null ? keyListener.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event) : super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onSearchRequested() {
        Intent searchIntent = new Intent(this, SearchActivity.class);
        startActivity(searchIntent);
        return true;
    }
}
