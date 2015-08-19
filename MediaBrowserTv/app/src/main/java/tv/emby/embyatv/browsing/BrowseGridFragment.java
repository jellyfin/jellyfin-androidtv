package tv.emby.embyatv.browsing;

import android.app.Fragment;
import android.os.Bundle;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.DisplayPreferences;
import mediabrowser.model.querying.ItemFields;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 8/16/2015.
 */
public class BrowseGridFragment extends StdGridFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        StdItemQuery query = new StdItemQuery(new ItemFields[] {ItemFields.PrimaryImageAspectRatio});
        query.setParentId(mParentId);
        if (mFolder.getType().equals("UserView")) {
            String type = mFolder.getCollectionType() != null ? mFolder.getCollectionType().toLowerCase() : "";
            query.setRecursive(true);
            switch (type) {
                case "movies":
                    query.setIncludeItemTypes(new String[]{"Movie"});
                    break;
                case "tvshows":
                    query.setIncludeItemTypes(new String[]{"Series"});
                    break;
            }
        }

        mRowDef = new BrowseRowDef("", query, 150, false, true);

        loadGrid(mRowDef);
    }

    @Override
    protected void setupEventListeners() {
        super.setupEventListeners();
        mGridAdapter.setRetrieveFinishedResponse(new EmptyResponse() {
            @Override
            public void onResponse() {
                setStatusText(mFolder.getName());
                updateCounter(mGridAdapter.getTotalItems() > 0 ? 1 : 0);
                setItem(null);
                setTitle(mFolder.getName());
            }
        });
    }

}
