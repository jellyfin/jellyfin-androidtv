package tv.emby.embyatv.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 12/28/2014.
 */
public class DelayedMessage {
    private String title = TvApp.getApplication().getString(R.string.lbl_please_wait);
    private String message = TvApp.getApplication().getString(R.string.msg_little_longer);
    private Runnable runnable;
    private ProgressDialog dialog;
    private Handler handler;

    public DelayedMessage(final Context activity) {
        this(activity, 750);
    }
    public DelayedMessage(final Context activity, int delay) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (activity != null) dialog = ProgressDialog.show(activity, title, message);
            }
        };
        handler.postDelayed(runnable, delay);
    }

    public void Cancel() {
        handler.removeCallbacks(runnable);
        if (dialog != null) dialog.dismiss();
    }
}
