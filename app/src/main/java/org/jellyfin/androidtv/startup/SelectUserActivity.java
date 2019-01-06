package org.jellyfin.androidtv.startup;

import android.app.Activity;
import android.os.Bundle;

import org.jellyfin.androidtv.R;


public class SelectUserActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);
    }
}
