package tv.emby.embyatv.browsing;

import android.os.Bundle;

import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 8/16/2015.
 */
public class BrowseGridFragment extends StdGridFragment {

    private String mParentId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mParentId = getActivity().getIntent().getStringExtra("ParentId");
        MainTitle = getActivity().getIntent().getStringExtra("Title");
        ShowBadge = MainTitle == null;
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IGridLoader gridLoader) {
        StdItemQuery query = new StdItemQuery();
        query.setParentId(mParentId);
        mRowDef = new BrowseRowDef("", query, 150, false, true);

        loadGrid(mRowDef);
    }
}
