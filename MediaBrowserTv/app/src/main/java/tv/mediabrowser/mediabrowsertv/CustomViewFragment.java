package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;

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
