package org.jellyfin.androidtv.startup;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class SelectUserActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SelectUserFragment())
                .commit();
    }
}
