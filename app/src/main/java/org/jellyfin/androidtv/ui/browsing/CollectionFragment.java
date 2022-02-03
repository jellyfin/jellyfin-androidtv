package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.querying.ItemFields;

public class CollectionFragment extends EnhancedBrowseFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(RowLoader rowLoader) {
        if (Utils.getSafeValue(mFolder.getChildCount(), 0) > 0) {
            StdItemQuery movies = new StdItemQuery(new ItemFields[]{
                    ItemFields.PrimaryImageAspectRatio,
                    ItemFields.Overview,
                    ItemFields.ItemCounts,
                    ItemFields.DisplayPreferencesId,
                    ItemFields.ChildCount,
                    ItemFields.MediaStreams,
                    ItemFields.MediaSources
            });
            movies.setParentId(mFolder.getId());
            movies.setIncludeItemTypes(new String[]{"Movie"});
            mRows.add(new BrowseRowDef(TvApp.getApplication().getString(R.string.lbl_movies), movies, 100));

            StdItemQuery series = new StdItemQuery();
            series.setParentId(mFolder.getId());
            series.setIncludeItemTypes(new String[]{"Series"});
            mRows.add(new BrowseRowDef(TvApp.getApplication().getString(R.string.lbl_tv_series), series, 100));

            StdItemQuery others = new StdItemQuery();
            others.setParentId(mFolder.getId());
            others.setExcludeItemTypes(new String[]{"Movie", "Series"});
            mRows.add(new BrowseRowDef(TvApp.getApplication().getString(R.string.lbl_other), others, 100));


            rowLoader.loadRows(mRows);
        }


    }


}
