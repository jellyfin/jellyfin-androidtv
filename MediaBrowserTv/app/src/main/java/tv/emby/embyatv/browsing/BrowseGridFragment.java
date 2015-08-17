package tv.emby.embyatv.browsing;

import android.os.Bundle;

import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 8/16/2015.
 */
public class BrowseGridFragment extends StdGridFragment {

    private String mParentId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mParentId = getActivity().getIntent().getStringExtra("ParentId");
        MainTitle = getActivity().getIntent().getStringExtra("Title");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupUIElements() {
        super.setupUIElements();

        TvApp.getApplication().getLogger().Debug("Parent ID is: "+mParentId);

        // calculate number of rows based on card height
        getGridPresenter().setNumberOfRows(getGridHeight() / getCardHeight());
    }

    @Override
    protected void setupQueries(IGridLoader gridLoader) {
        StdItemQuery query = new StdItemQuery();
        query.setParentId(mParentId);
        mRowDef = new BrowseRowDef("", query, 150, false, true);

        loadGrid(mRowDef);
    }
}
