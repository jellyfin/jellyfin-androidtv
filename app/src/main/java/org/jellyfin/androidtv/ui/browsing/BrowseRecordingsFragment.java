package org.jellyfin.androidtv.ui.browsing;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Row;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTitle.setText(getString(R.string.lbl_loading_elipses));
    }

    @Override
    protected void setupQueries(final RowLoader rowLoader) {
        showViews = true;
        //Latest Recordings
        RecordingQuery recordings = new RecordingQuery();
        recordings.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        recordings.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        recordings.setEnableImages(true);
        recordings.setLimit(40);
        mRows.add(new BrowseRowDef(getString(R.string.lbl_recent_recordings), recordings, 40));

        //Movies
        RecordingQuery movies = new RecordingQuery();
        movies.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        movies.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        movies.setEnableImages(true);
        movies.setIsMovie(true);
        BrowseRowDef moviesDef = new BrowseRowDef(getString(R.string.lbl_movies), movies, 60);

        //Shows
        RecordingQuery shows = new RecordingQuery();
        shows.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        shows.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        shows.setEnableImages(true);
        shows.setIsSeries(true);
        BrowseRowDef showsDef = new BrowseRowDef(getString(R.string.lbl_tv_series), shows, 60);

        mRows.add(showsDef);
        mRows.add(moviesDef);

        //Sports
        RecordingQuery sports = new RecordingQuery();
        sports.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        sports.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        sports.setEnableImages(true);
        sports.setIsSports(true);
        mRows.add(new BrowseRowDef(getString(R.string.lbl_sports), sports, 60));

        //Kids
        RecordingQuery kids = new RecordingQuery();
        kids.setFields(new ItemFields[]{
                ItemFields.Overview,
                ItemFields.PrimaryImageAspectRatio,
                ItemFields.ChildCount
        });
        kids.setUserId(KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString());
        kids.setEnableImages(true);
        kids.setIsKids(true);
        mRows.add(new BrowseRowDef(getString(R.string.lbl_kids), kids, 60));

        rowLoader.loadRows(mRows);
        addNext24Timers();
    }

    private void addNext24Timers() {
        final TimerQuery scheduled = new TimerQuery();
        final long ticks24 = 1000 * 60 * 60 * 24;
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvTimersAsync(scheduled, new LifecycleAwareResponse<TimerInfoDtoResult>(getLifecycle()) {
            @Override
            public void onResponse(TimerInfoDtoResult response) {
                if (!getActive()) return;

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
                    ItemRowAdapter scheduledAdapter = new ItemRowAdapter(requireContext(), nearTimers, mCardPresenter, mRowsAdapter, true);
                    scheduledAdapter.Retrieve();
                    ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                    mRowsAdapter.add(0, scheduleRow);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                            mRowsFragment.setSelectedPosition(0, true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onError(Exception exception) {
                if (!getActive()) return;

                Utils.showToast(getContext(), exception.getLocalizedMessage());
            }

        });
    }

    @Override
    protected void addAdditionalRows(MutableObjectAdapter<Row> rowAdapter) {
        HeaderItem gridHeader = new HeaderItem(rowAdapter.size(), getString(R.string.lbl_views));

        GridButtonPresenter mGridPresenter = new GridButtonPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new GridButton(SCHEDULE, getString(R.string.lbl_schedule)));
        gridRowAdapter.add(new GridButton(SERIES, getString(R.string.lbl_series_recordings)));
        rowAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }
}
