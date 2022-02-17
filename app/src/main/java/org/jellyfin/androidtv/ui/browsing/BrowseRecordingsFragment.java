package org.jellyfin.androidtv.ui.browsing;

import android.os.Handler;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.livetv.RecordingGroupQuery;
import org.jellyfin.apiclient.model.livetv.RecordingQuery;
import org.jellyfin.apiclient.model.livetv.TimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.ItemFields;
import org.jellyfin.apiclient.model.results.TimerInfoDtoResult;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class BrowseRecordingsFragment extends EnhancedBrowseFragment {
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void setupQueries(final RowLoader rowLoader) {
        showViews = true;
        mTitle.setText(getString(R.string.lbl_loading_elipses));
        //Latest Recordings
        RecordingQuery recordings = new RecordingQuery();
        recordings.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
        recordings.setEnableImages(true);
        recordings.setLimit(40);
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_recent_recordings), recordings, 40));

        //Movies
        RecordingQuery movies = new RecordingQuery();
        movies.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        movies.setUserId(TvApp.getApplication().getCurrentUser().getId());
        movies.setEnableImages(true);
        movies.setIsMovie(true);
        BrowseRowDef moviesDef = new BrowseRowDef(mActivity.getString(R.string.lbl_movies), movies, 60);

        //Shows
        RecordingQuery shows = new RecordingQuery();
        shows.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        shows.setUserId(TvApp.getApplication().getCurrentUser().getId());
        shows.setEnableImages(true);
        shows.setIsSeries(true);
        BrowseRowDef showsDef = new BrowseRowDef(mActivity.getString(R.string.lbl_tv_series), shows, 60);

        mRows.add(showsDef);
        mRows.add(moviesDef);

        //Sports
        RecordingQuery sports = new RecordingQuery();
        sports.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        sports.setUserId(TvApp.getApplication().getCurrentUser().getId());
        sports.setEnableImages(true);
        sports.setIsSports(true);
        mRows.add(new BrowseRowDef(mActivity.getString(R.string.lbl_sports), sports, 60));

        //Kids
        RecordingQuery kids = new RecordingQuery();
        kids.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
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
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvTimersAsync(scheduled, new Response<TimerInfoDtoResult>() {
            @Override
            public void onResponse(TimerInfoDtoResult response) {
                List<BaseItemDto> nearTimers = new ArrayList<>();
                long next24 = System.currentTimeMillis() + ticks24;
                //Get scheduled items for next 24 hours
                for (TimerInfoDto timer : response.getItems()) {
                    if (TimeUtils.convertToLocalDate(timer.getStartDate()).getTime() <= next24) {
                        BaseItemDto programInfo = timer.getProgramInfo();
                        if (programInfo == null) {
                            programInfo = new BaseItemDto();
                            programInfo.setId(timer.getId());
                            programInfo.setChannelName(timer.getChannelName());
                            programInfo.setName(Utils.getSafeValue(timer.getName(), "Unknown"));
                            Timber.w("No program info for timer %s.  Creating one...", programInfo.getName());
                            programInfo.setBaseItemType(BaseItemType.Program);
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
                    Utils.showToast(getContext(), exception.getLocalizedMessage());
                    }

        });
    }

    @Override
    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {
        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), getString(R.string.lbl_views));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(SCHEDULE, getString(R.string.lbl_schedule), R.drawable.tile_port_time, null));
        gridRowAdapter.add(new GridButton(SERIES, mActivity.getString(R.string.lbl_series_recordings), R.drawable.tile_port_series_timer, null));
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }
}
