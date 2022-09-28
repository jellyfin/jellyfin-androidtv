package org.jellyfin.androidtv.ui.shared;

import androidx.fragment.app.FragmentActivity;

import org.jellyfin.androidtv.constant.CustomMessage;

import kotlin.Deprecated;
import kotlin.ReplaceWith;

@Deprecated(message = "Use FragmentActivity instead", replaceWith = @ReplaceWith(expression = "FragmentActivity", imports = {}))
public abstract class BaseActivity extends FragmentActivity {
    private MessageListener messageListener;

    public BaseActivity() {
        super();
    }

    public BaseActivity(int fragmentContentView) {
        super(fragmentContentView);
    }

    public void registerMessageListener(MessageListener listener) {
        messageListener = listener;
    }

    public void removeMessageListener() {
        messageListener = null;
    }

    public void sendMessage(CustomMessage message) {
        if (messageListener != null) {
            messageListener.onMessageReceived(message);
        }
    }
}
