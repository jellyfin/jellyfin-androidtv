package org.jellyfin.androidtv.ui.shared;

import android.view.KeyEvent;

import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.constant.CustomMessage;

import kotlin.Deprecated;

@Deprecated(message = "Use FragmentActivity instead")
public abstract class BaseActivity extends FragmentActivity {
    private IKeyListener keyListener;
    private IMessageListener messageListener;

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
}
