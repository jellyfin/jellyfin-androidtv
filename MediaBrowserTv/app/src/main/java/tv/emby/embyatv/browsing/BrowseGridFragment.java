package tv.emby.embyatv.browsing;

import android.os.Bundle;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 8/16/2015.
 */
public class BrowseGridFragment extends StdGridFragment {

    private String mParentId;
    private BaseItemDto mFolder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"), BaseItemDto.class);
        mParentId = mFolder.getId();
        MainTitle = mFolder.getName();
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
}
