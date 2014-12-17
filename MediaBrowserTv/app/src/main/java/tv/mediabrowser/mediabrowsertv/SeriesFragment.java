package tv.mediabrowser.mediabrowsertv;

import android.os.Bundle;

import java.util.Arrays;

import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.NextUpQuery;
import mediabrowser.model.querying.SeasonQuery;
import mediabrowser.model.querying.UpcomingEpisodesQuery;

/**
 * Created by Eric on 12/4/2014.
 */
public class SeriesFragment extends BrowseFolderFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void setupQueries(IRowLoader rowLoader) {


        SeasonQuery seasons = new SeasonQuery();
        seasons.setSeriesId(mFolder.getId());
        seasons.setUserId(TvApp.getApplication().getCurrentUser().getId());
        mRows.add(new BrowseRowDef("Seasons", seasons));

        NextUpQuery nextUpQuery = new NextUpQuery();
        nextUpQuery.setUserId(TvApp.getApplication().getCurrentUser().getId());
        nextUpQuery.setSeriesId(mFolder.getId());
        nextUpQuery.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
        mRows.add(new BrowseRowDef("Next Up", nextUpQuery));

        UpcomingEpisodesQuery upcoming = new UpcomingEpisodesQuery();
        upcoming.setUserId(TvApp.getApplication().getCurrentUser().getId());
        upcoming.setParentId(mFolder.getId());
        upcoming.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio});
        mRows.add(new BrowseRowDef("Upcoming", upcoming));

        setHeadersState(HEADERS_DISABLED);

        rowLoader.loadRows(mRows);

    }


}
