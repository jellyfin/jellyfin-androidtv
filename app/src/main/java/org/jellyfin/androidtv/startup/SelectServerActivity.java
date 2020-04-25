package org.jellyfin.androidtv.startup;

import android.os.Bundle;

import org.jellyfin.androidtv.base.BaseActivity;

public class SelectServerActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SelectServerFragment())
                .commit();
    }
}
