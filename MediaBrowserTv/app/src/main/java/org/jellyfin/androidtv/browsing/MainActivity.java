package org.jellyfin.androidtv.browsing;

import android.os.Bundle;

import java.io.IOException;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.startup.LogonCredentials;
import org.jellyfin.androidtv.util.Utils;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}
