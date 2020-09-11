package org.jellyfin.androidtv.ui.home;

import org.jellyfin.androidtv.ui.browsing.BrowseRowDef;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.constants.QueryType;
import org.jellyfin.androidtv.data.querying.ViewQuery;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

public class HomeFragmentBrowseRowDefRow extends HomeFragmentRow {
    private BrowseRowDef browseRowDef;

    public HomeFragmentBrowseRowDefRow(BrowseRowDef browseRowDef) {
        this.browseRowDef = browseRowDef;
    }

    @Override
    public void addToRowsAdapter(CardPresenter cardPresenter, ArrayObjectAdapter rowsAdapter) {
        HeaderItem header = new HeaderItem(browseRowDef.getHeaderText());

        ItemRowAdapter rowAdapter;
        switch (browseRowDef.getQueryType()) {
            case NextUp:
                rowAdapter = new ItemRowAdapter(browseRowDef.getNextUpQuery(), true, cardPresenter, rowsAdapter);
                break;
            case LatestItems:
                rowAdapter = new ItemRowAdapter(browseRowDef.getLatestItemsQuery(), true, cardPresenter, rowsAdapter);
                break;
            case Season:
                rowAdapter = new ItemRowAdapter(browseRowDef.getSeasonQuery(), cardPresenter, rowsAdapter);
                break;
            case Upcoming:
                rowAdapter = new ItemRowAdapter(browseRowDef.getUpcomingQuery(), cardPresenter, rowsAdapter);
                break;
            case Views:
                rowAdapter = new ItemRowAdapter(new ViewQuery(), cardPresenter, rowsAdapter);
                break;
            case SimilarSeries:
                rowAdapter = new ItemRowAdapter(browseRowDef.getSimilarQuery(), QueryType.SimilarSeries, cardPresenter, rowsAdapter);
                break;
            case SimilarMovies:
                rowAdapter = new ItemRowAdapter(browseRowDef.getSimilarQuery(), QueryType.SimilarMovies, cardPresenter, rowsAdapter);
                break;
            case Persons:
                rowAdapter = new ItemRowAdapter(browseRowDef.getPersonsQuery(), browseRowDef.getChunkSize(), cardPresenter, rowsAdapter);
                break;
            case LiveTvChannel:
                rowAdapter = new ItemRowAdapter(browseRowDef.getTvChannelQuery(), 40, cardPresenter, rowsAdapter);
                break;
            case LiveTvProgram:
                rowAdapter = new ItemRowAdapter(browseRowDef.getProgramQuery(), cardPresenter, rowsAdapter);
                break;
            case LiveTvRecording:
                rowAdapter = new ItemRowAdapter(browseRowDef.getRecordingQuery(), browseRowDef.getChunkSize(), cardPresenter, rowsAdapter);
                break;
            case LiveTvRecordingGroup:
                rowAdapter = new ItemRowAdapter(browseRowDef.getRecordingGroupQuery(), cardPresenter, rowsAdapter);
                break;
            default:
                rowAdapter = new ItemRowAdapter(browseRowDef.getQuery(), browseRowDef.getChunkSize(), browseRowDef.getPreferParentThumb(), browseRowDef.isStaticHeight(), cardPresenter, rowsAdapter, browseRowDef.getQueryType());
                break;
        }

        rowAdapter.setReRetrieveTriggers(browseRowDef.getChangeTriggers());

        ListRow row = new ListRow(header, rowAdapter);
        rowAdapter.setRow(row);
        rowAdapter.Retrieve();

        rowsAdapter.add(row);
    }
}
