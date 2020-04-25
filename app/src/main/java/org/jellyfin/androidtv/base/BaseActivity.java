package org.jellyfin.androidtv.base;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import org.jellyfin.androidtv.TvApp;

import androidx.fragment.app.FragmentActivity;

public abstract class BaseActivity extends FragmentActivity {
    private IKeyListener keyListener;
    private IMessageListener messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TvApp.getApplication().setCurrentActivity(this);
    }

    //Banish task bars and navigation controls on non-TV devices
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && android.os.Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
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

    public void registerKeyListener(IKeyListener listener) {
        keyListener = listener;
    }

    public void registerMessageListener(IMessageListener listener) {
        messageListener = listener;
    }

    public void sendMessage(CustomMessage message) {
        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return keyListener != null ? keyListener.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event) : super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onSearchRequested() {
        TvApp.getApplication().showSearch(this, false);
        return true;
    }
}
