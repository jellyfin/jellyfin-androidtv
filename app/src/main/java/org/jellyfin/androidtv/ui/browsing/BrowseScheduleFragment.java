package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;

public class BrowseScheduleFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void setupQueries(final RowLoader rowLoader) {
        TvManager.getScheduleRowsAsync(this, null, new CardPresenter(true), mRowsAdapter);
    }
}
