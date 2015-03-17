package tv.emby.embyatv.settings;

import android.os.Bundle;

import tv.emby.embyatv.R;
import tv.emby.embyatv.base.BaseActivity;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
    }
}
