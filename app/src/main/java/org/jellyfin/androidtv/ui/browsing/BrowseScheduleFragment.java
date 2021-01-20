package org.jellyfin.androidtv.ui.browsing;

import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.livetv.TimerQuery;

public class BrowseScheduleFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        TvManager.getScheduleRowsAsync(requireContext(), new TimerQuery(), new CardPresenter(true), mRowsAdapter, new Response<Integer>() {
            @Override
            public void onResponse(Integer response) {
                if (response == 0) mActivity.setTitle("No Scheduled Recordings");
            }
        });
    }
}
