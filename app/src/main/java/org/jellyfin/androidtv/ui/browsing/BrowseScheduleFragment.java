package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.apiclient.model.livetv.TimerQuery;

public class BrowseScheduleFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void setupQueries(final RowLoader rowLoader) {
        TvManager.getScheduleRowsAsync(requireContext(), new TimerQuery(), new CardPresenter(true), mRowsAdapter, new LifecycleAwareResponse<Integer>(getLifecycle()) {
        });
    }
}
