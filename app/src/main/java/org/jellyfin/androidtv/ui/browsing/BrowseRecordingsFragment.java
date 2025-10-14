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
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.TimerInfoDto;

import java.time.LocalDateTime;
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
        mRows.add(new BrowseRowDef(getString(R.string.lbl_recent_recordings), BrowsingUtils.createLiveTVRecordingsRequest(40), 40));

        //Movies
        BrowseRowDef moviesDef = new BrowseRowDef(getString(R.string.lbl_movies), BrowsingUtils.createLiveTVMovieRecordingsRequest(), 60);

        //Shows
        BrowseRowDef showsDef = new BrowseRowDef(getString(R.string.lbl_tv_series), BrowsingUtils.createLiveTVSeriesRecordingsRequest(), 60);

        mRows.add(showsDef);
        mRows.add(moviesDef);

        //Sports
        mRows.add(new BrowseRowDef(getString(R.string.lbl_sports), BrowsingUtils.createLiveTVSportsRecordingsRequest(), 60));

        //Kids
        mRows.add(new BrowseRowDef(getString(R.string.lbl_kids), BrowsingUtils.createLiveTVKidsRecordingsRequest(), 60));

        rowLoader.loadRows(mRows);
        addNext24Timers();
    }

    private void addNext24Timers() {
        BrowseViewFragmentHelperKt.getLiveTvTimers(this, timers -> {
            List<BaseItemDto> nearTimers = new ArrayList<>();
            LocalDateTime next24 = LocalDateTime.now().plusDays(1);
            //Get scheduled items for next 24 hours
            for (TimerInfoDto timer : timers.getItems()) {
                if (timer.getStartDate().isBefore(next24)) {
                    nearTimers.add(BrowseViewFragmentHelperKt.getTimerProgramInfo(timer));
                }
            }
            if (!nearTimers.isEmpty()) {
                ItemRowAdapter scheduledAdapter = new ItemRowAdapter(requireContext(), nearTimers, mCardPresenter, mRowsAdapter, true);
                scheduledAdapter.Retrieve();
                ListRow scheduleRow = new ListRow(new HeaderItem(getString(R.string.scheduled_in_next_24_hours)), scheduledAdapter);
                mRowsAdapter.add(0, scheduleRow);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            return;

                        mRowsFragment.setSelectedPosition(0, true);
                    }
                }, 500);
            }
            return null;
        }, exception -> {
            Timber.e(exception, "Failed to get Live TV recordings / timers");
            return null;
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
