package tv.emby.embyatv.browsing;

import android.os.Bundle;

import tv.emby.embyatv.TvApp;

/**
 * Created by Eric on 1/18/2015.
 */
public class CustomViewFragment extends BrowseFolderFragment {
    protected String includeType;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        includeType = getActivity().getIntent().getStringExtra("IncludeType");
        TvApp.getApplication().getLogger().Debug("Item type: "+includeType);
        showViews = false;

        super.onActivityCreated(savedInstanceState);
    }
}
