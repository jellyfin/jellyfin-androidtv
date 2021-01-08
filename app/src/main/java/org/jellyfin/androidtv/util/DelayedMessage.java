package org.jellyfin.androidtv.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;

public class DelayedMessage {
    private final String title;
    private final String message;
    private final Handler handler;
    private final Runnable runnable;
    private ProgressDialog dialog;

    public DelayedMessage(@NonNull final Context activity) {
        this(activity, 750);
    }

    public DelayedMessage(@NonNull final Context activity, int delay) {
        title = activity.getString(R.string.lbl_please_wait);
        message = activity.getString(R.string.msg_little_longer);

        handler = new Handler(activity.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                dialog = ProgressDialog.show(activity, title, message);
            }
        };
        handler.postDelayed(runnable, delay);
    }

    public void Cancel() {
        handler.removeCallbacks(runnable);
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
