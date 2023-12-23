package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.querying.StdItemQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.sdk.model.api.BaseItemKind;

public class CollectionFragment extends EnhancedBrowseFragment {
    @Override
    protected void setupQueries(RowLoader rowLoader) {
        StdItemQuery movies = new StdItemQuery(new ItemFields[]{
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.Overview,
                ItemFields.ItemCounts,
                ItemFields.DisplayPreferencesId,
                ItemFields.ChildCount,
                ItemFields.MediaStreams,
                ItemFields.MediaSources
        });
        movies.setParentId(mFolder.getId().toString());
        movies.setIncludeItemTypes(new String[]{BaseItemKind.MOVIE.getSerialName()});
        mRows.add(new BrowseRowDef(getString(R.string.lbl_movies), movies, 100));

        StdItemQuery series = new StdItemQuery();
        series.setParentId(mFolder.getId().toString());
        series.setIncludeItemTypes(new String[]{BaseItemKind.SERIES.getSerialName()});
        mRows.add(new BrowseRowDef(getString(R.string.lbl_tv_series), series, 100));

        StdItemQuery others = new StdItemQuery();
        others.setParentId(mFolder.getId().toString());
        others.setExcludeItemTypes(new String[]{BaseItemKind.MOVIE.getSerialName(), BaseItemKind.SERIES.getSerialName()});
        mRows.add(new BrowseRowDef(getString(R.string.lbl_other), others, 100));

        rowLoader.loadRows(mRows);
    }
}
