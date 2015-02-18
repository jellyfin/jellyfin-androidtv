package tv.mediabrowser.mediabrowsertv.base;

import android.app.Activity;

import java.util.Calendar;

import tv.mediabrowser.mediabrowsertv.TvApp;

/**
 * Created by Eric on 2/18/2015.
 */
public class BaseActivity extends Activity {

    private TvApp app = TvApp.getApplication();

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        app.setLastUserInteraction(Calendar.getInstance());
    }
}
