package org.jellyfin.androidtv.base;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.Utils;

public abstract class BaseActivity extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private TvApp app = TvApp.getApplication();
    private Handler handler = new Handler();
    private IKeyListener keyListener;
    private IMessageListener messageListener;

    private View messageUi;
    private TextView messageTitle;
    private TextView messageMessage;
    private ImageView messageIcon;
    private int messageTimeout = 6000;
    private int farRight;
    private int msgPos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TvApp.getApplication().setCurrentActivity(this);

        //Add message UI - delay to ensure on top of other views
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && app.getCurrentActivity() != null) {
                    FrameLayout root = findViewById(android.R.id.content);
                    messageUi = View.inflate(app.getCurrentActivity(), R.layout.message, null);
                    messageTitle = messageUi.findViewById(R.id.msgTitle);
                    messageMessage = messageUi.findViewById(R.id.message);
                    messageIcon = messageUi.findViewById(R.id.msgIcon);
                    messageUi.setAlpha(0);
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.BOTTOM);
                    params.bottomMargin = Utils.convertDpToPixel(TvApp.getApplication().getCurrentActivity(), 50);
                    Rect windowSize = new Rect();
                    getWindow().getDecorView().getWindowVisibleDisplayFrame(windowSize);
                    farRight = windowSize.right;
                    msgPos = farRight - Utils.convertDpToPixel(TvApp.getApplication().getCurrentActivity(), 500);
                    messageUi.setLeft(farRight);
                    root.addView(messageUi, params);
                }
            }
        }, 500);

    }

    public void showMessage(String title, String msg) {
        showMessage(title, msg, messageTimeout, null, null);
    }

    public void showMessage(String title, String msg, int timeout) {
        showMessage(title, msg, timeout, null, null);
    }

    public void showMessage(String title, String msg, int timeout, Integer icon, Integer background) {
        if (messageUi != null && !isFinishing()) {
            messageTitle.setText(title);
            messageMessage.setText(msg);
            if (icon != null) {
                messageIcon.setImageResource(icon);
            }
            if (background != null) {
                messageUi.setBackgroundResource(background);
            }
            messageUi.animate().x(msgPos).alpha(1).setDuration(300);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (messageUi != null && !isFinishing()) {
                        messageUi.animate().alpha(0).x(farRight).setDuration(300);
                    }
                }
            }, timeout);
        }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
