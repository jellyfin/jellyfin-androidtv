package org.jellyfin.androidtv.startup;

import android.os.Bundle;

import org.jellyfin.androidtv.R;

import androidx.fragment.app.FragmentActivity;


public class SelectUserActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);
    }
}
