package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.constants.Extras;

import timber.log.Timber;

public class CustomViewFragment extends BrowseFolderFragment {
    protected String includeType;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        includeType = getActivity().getIntent().getStringExtra(Extras.IncludeType);
        Timber.d("Item type: %s", includeType);
        showViews = false;

        super.onActivityCreated(savedInstanceState);
    }
}
