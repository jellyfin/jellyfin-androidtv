package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;

/**
 * Created by Eric on 12/28/2014.
 */
public class DelayedMessage {
    private int delay = 750;
    private String title = "Please Wait";
    private String message = "This is taking a little longer than expected...";
    private Runnable runnable;
    private ProgressDialog dialog;
    private Handler handler;

    public DelayedMessage(final Activity activity) {
        handler = new Handler();
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
        if (dialog != null) dialog.dismiss();
    }
}
