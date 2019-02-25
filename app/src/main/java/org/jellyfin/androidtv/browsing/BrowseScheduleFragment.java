package org.jellyfin.androidtv.browsing;

import android.os.Handler;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
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
import mediabrowser.model.results.TimerInfoDtoResult;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.androidtv.util.Utils;

/**
 * Created by Eric on 9/3/2015.
 */
public class BrowseScheduleFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {
        TvManager.getScheduleRowsAsync(new TimerQuery(), new CardPresenter(true), mRowsAdapter, new Response<Integer>() {
            @Override
            public void onResponse(Integer response) {
                if (response == 0) mActivity.setTitle("No Scheduled Recordings");
            }
        });

    }


}
