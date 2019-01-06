package org.jellyfin.androidtv.settings;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.base.BaseActivity;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);
    }
}
