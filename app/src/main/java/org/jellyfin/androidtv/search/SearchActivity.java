package org.jellyfin.androidtv.search;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.base.BaseActivity;

public class SearchActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);
    }

    @Override
    public boolean onSearchRequested() {
        // Reset layout
        recreate();

        return true;
    }
}
