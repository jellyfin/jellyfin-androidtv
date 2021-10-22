package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import timber.log.Timber;

public class CustomViewFragment extends BrowseFolderFragment {
    protected String includeType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        includeType = getActivity().getIntent().getStringExtra(GroupedItemsActivity.EXTRA_INCLUDE_TYPE);
        Timber.d("Item type: %s", includeType);

        super.onCreate(savedInstanceState);
    }
}
