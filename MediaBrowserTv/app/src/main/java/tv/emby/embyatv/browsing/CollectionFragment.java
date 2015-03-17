package tv.emby.embyatv.browsing;

import android.os.Bundle;

import tv.emby.embyatv.R;
import tv.emby.embyatv.querying.StdItemQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class CollectionFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {

        StdItemQuery movies = new StdItemQuery();
        movies.setParentId(mFolder.getId());
        movies.setIncludeItemTypes(new String[] {"Movie"});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_movies), movies, 100));

        StdItemQuery series = new StdItemQuery();
        series.setParentId(mFolder.getId());
        series.setIncludeItemTypes(new String[]{"Series"});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_tv_series), series, 100));

        StdItemQuery others = new StdItemQuery();
        others.setParentId(mFolder.getId());
        others.setExcludeItemTypes(new String[]{"Movie","Series"});
        mRows.add(new BrowseRowDef(mApplication.getString(R.string.lbl_other), others, 100));

        setHeadersState(HEADERS_DISABLED);

        rowLoader.loadRows(mRows);

    }


}
