package org.jellyfin.androidtv.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.base.BaseActivity;

public class CollectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
    }
}
