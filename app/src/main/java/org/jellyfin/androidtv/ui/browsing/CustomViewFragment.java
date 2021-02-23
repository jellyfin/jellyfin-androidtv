package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.constant.Extras;

import timber.log.Timber;

public class CustomViewFragment extends BrowseFolderFragment {
    protected String includeType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        includeType = getActivity().getIntent().getStringExtra(Extras.IncludeType);
        Timber.d("Item type: %s", includeType);
        showViews = false;

        super.onCreate(savedInstanceState);
    }
}
