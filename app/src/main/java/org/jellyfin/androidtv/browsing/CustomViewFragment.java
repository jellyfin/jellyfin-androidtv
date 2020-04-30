package org.jellyfin.androidtv.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constants.Extras;

public class CustomViewFragment extends BrowseFolderFragment {
    protected String includeType;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        includeType = getActivity().getIntent().getStringExtra(Extras.IncludeType);
        TvApp.getApplication().getLogger().Debug("Item type: %s", includeType);
        showViews = false;

        super.onActivityCreated(savedInstanceState);
    }
}
