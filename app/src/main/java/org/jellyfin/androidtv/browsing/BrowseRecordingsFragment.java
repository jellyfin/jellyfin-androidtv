package org.jellyfin.androidtv.browsing;

import android.os.Handler;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.livetv.RecordingGroupQuery;
import mediabrowser.model.livetv.RecordingQuery;
import mediabrowser.model.livetv.TimerInfoDto;
import mediabrowser.model.livetv.TimerQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.results.TimerInfoDtoResult;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.model.DisplayPriorityType;
import org.jellyfin.androidtv.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 9/3/2015.
 */
public class BrowseRecordingsFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {

        showViews = true;
        mTitle.setText(TvApp.getApplication().getResources().getString(R.string.lbl_loading_elipses));
        //Latest Recordings
        RecordingQuery recordings = new RecordingQuery();
        recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
        recordings.setEnableImages(true);
        recordings.setLimit(40);
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_recent_recordings), recordings, 40));

        //Movies
        RecordingQuery movies = new RecordingQuery();
        movies.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        movies.setUserId(TvApp.getApplication().getCurrentUser().getId());
        movies.setEnableImages(true);
        movies.setIsMovie(true);
        BrowseRowDef moviesDef = new BrowseRowDef(mActivity.getString(R.string.lbl_movies), movies, 60);

        //Shows
        RecordingQuery shows = new RecordingQuery();
        shows.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        shows.setUserId(TvApp.getApplication().getCurrentUser().getId());
        shows.setEnableImages(true);
        shows.setIsSeries(true);
        BrowseRowDef showsDef = new BrowseRowDef(mActivity.getString(R.string.lbl_tv_series), shows, 60);

        //Insert order based on pref
        if (mApplication.getDisplayPriority() == DisplayPriorityType.Movies) {
            mRows.add(moviesDef);
            mRows.add(showsDef);
        } else {
            mRows.add(showsDef);
            mRows.add(moviesDef);
        }

        //Sports
        RecordingQuery sports = new RecordingQuery();
        sports.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        sports.setUserId(TvApp.getApplication().getCurrentUser().getId());
        sports.setEnableImages(true);
        sports.setIsSports(true);
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_sports), sports, 60));

        //Kids
        RecordingQuery kids = new RecordingQuery();
        kids.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        kids.setUserId(TvApp.getApplication().getCurrentUser().getId());
        kids.setEnableImages(true);
        kids.setIsKids(true);
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_kids), kids, 60));

        //All Recordings by group - will only be there for non-internal TV
        RecordingGroupQuery recordingGroups = new RecordingGroupQuery();
        recordingGroups.setUserId(TvApp.getApplication().getCurrentUser().getId());
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_all_recordings), recordingGroups));

        rowLoader.loadRows(mRows);
        addNext24Timers();
    }

    private void addNext24Timers() {
        final TimerQuery scheduled = new TimerQuery();
        final long ticks24 = 1000 * 60 * 60 * 24;
        TvApp.getApplication().getApiClient().GetLiveTvTimersAsync(scheduled, new Response<TimerInfoDtoResult>() {
            @Override
            public void onResponse(TimerInfoDtoResult response) {
                List<BaseItemDto> nearTimers = new ArrayList<>();
                long next24 = System.currentTimeMillis() + ticks24;
                //Get scheduled items for next 24 hours
                for (TimerInfoDto timer : response.getItems()) {
                    if (Utils.convertToLocalDate(timer.getStartDate()).getTime() <= next24) {
                        BaseItemDto programInfo = timer.getProgramInfo();
                        if (programInfo == null) {
                            programInfo = new BaseItemDto();
                            programInfo.setId(timer.getId());
                            programInfo.setChannelName(timer.getChannelName());
                            programInfo.setName(Utils.NullCoalesce(timer.getName(), "Unknown"));
                            TvApp.getApplication().getLogger().Warn("No program info for timer %s.  Creating one...", programInfo.getName());
                            programInfo.setType("Program");
                            programInfo.setTimerId(timer.getId());
                            programInfo.setSeriesTimerId(timer.getSeriesTimerId());
                            programInfo.setStartDate(timer.getStartDate());
                            programInfo.setEndDate(timer.getEndDate());
                        }
                        programInfo.setLocationType(LocationType.Virtual);
                        nearTimers.add(programInfo);
                    }
                }
                if (nearTimers.size() > 0) {
                    ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                    scheduledAdapter.Retrieve();
                    ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                    mRowsAdapter.add(0, scheduleRow);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRowsFragment.setSelectedPosition(0, true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onError(Exception exception) {
                    Utils.showToast(mApplication, exception.getLocalizedMessage());
                    }

        });

    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), mApplication.getString(R.string.lbl_views));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(SCHEDULE, TvApp.getApplication().getString(R.string.lbl_schedule), R.drawable.clock));
        gridRowAdapter.add(new GridButton(SERIES, mActivity.getString(R.string.lbl_series_recordings), R.drawable.seriestimerp));
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));

    }
}
