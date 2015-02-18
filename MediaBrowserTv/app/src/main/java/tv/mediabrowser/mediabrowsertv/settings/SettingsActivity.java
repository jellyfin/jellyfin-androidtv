package tv.mediabrowser.mediabrowsertv.settings;

import android.app.Activity;
import android.os.Bundle;

import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.base.BaseActivity;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
    }
}
