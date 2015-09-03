package tv.emby.embyatv.browsing;

import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;

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
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 9/3/2015.
 */
public class BrowseRecordingsFragment extends EnhancedBrowseFragment {

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null && !getActivity().isFinishing() && mCurrentRow != null && mCurrentItem != null && mCurrentItem.getItemId().equals(TvApp.getApplication().getLastDeletedItemId())) {
            ((ItemRowAdapter)mCurrentRow.getAdapter()).remove(mCurrentItem);
        }
    }

    @Override
    protected void setupQueries(final IRowLoader rowLoader) {

        mTitle.setText(TvApp.getApplication().getResources().getString(R.string.lbl_loading_elipses));
        //Latest Recordings
        RecordingQuery recordings = new RecordingQuery();
        recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
        recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
        recordings.setEnableImages(true);
        recordings.setLimit(40);

        //Do a straight query and then split the returned items into logical groups
        TvApp.getApplication().getApiClient().GetLiveTvRecordingsAsync(recordings, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                final ItemsResult recordingsResponse = response;
                final long ticks24 = 1000 * 60 * 60 * 24;

                // Also get scheduled recordings for next 24 hours
                final TimerQuery scheduled = new TimerQuery();
                TvApp.getApplication().getApiClient().GetLiveTvTimersAsync(scheduled, new Response<TimerInfoDtoResult>(){
                    @Override
                    public void onResponse(TimerInfoDtoResult response) {
                        List<BaseItemDto> nearTimers = new ArrayList<>();
                        long next24 = System.currentTimeMillis() + ticks24;
                        //Get scheduled items for next 24 hours
                        for (TimerInfoDto timer : response.getItems()) {
                            if (Utils.convertToLocalDate(timer.getProgramInfo().getStartDate()).getTime() <= next24) {
                                timer.getProgramInfo().setLocationType(LocationType.Virtual);
                                nearTimers.add(timer.getProgramInfo());
                            }
                        }

                        if (recordingsResponse.getTotalRecordCount() > 0) {
                            List<BaseItemDto> dayItems = new ArrayList<>();
                            List<BaseItemDto> weekItems = new ArrayList<>();

                            long past24 = System.currentTimeMillis() - ticks24;
                            long pastWeek = System.currentTimeMillis() - (ticks24 * 7);
                            for (BaseItemDto item : recordingsResponse.getItems()) {
                                if (Utils.convertToLocalDate(item.getStartDate()).getTime() >= past24) {
                                    dayItems.add(item);
                                } else if (Utils.convertToLocalDate(item.getStartDate()).getTime() >= pastWeek) {
                                    weekItems.add(item);
                                }
                            }

                            //First put all recordings in and retrieve
                            //All Recordings by group
                            RecordingGroupQuery recordingGroups = new RecordingGroupQuery();
                            recordingGroups.setUserId(TvApp.getApplication().getCurrentUser().getId());
                            mRows.add(new BrowseRowDef("All Recordings", recordingGroups));
                            rowLoader.loadRows(mRows);

                            //Now insert our smart rows
                            if (weekItems.size() > 0) {
                                ItemRowAdapter weekAdapter = new ItemRowAdapter(weekItems, mCardPresenter, mRowsAdapter, true);
                                weekAdapter.Retrieve();
                                ListRow weekRow = new ListRow(new HeaderItem("Past Week", null), weekAdapter);
                                mRowsAdapter.add(0, weekRow);
                            }
                            if (nearTimers.size() > 0) {
                                ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                scheduledAdapter.Retrieve();
                                ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours", null), scheduledAdapter);
                                mRowsAdapter.add(0, scheduleRow);
                            }
                            if (dayItems.size() > 0) {
                                ItemRowAdapter dayAdapter = new ItemRowAdapter(dayItems, mCardPresenter, mRowsAdapter, true);
                                dayAdapter.Retrieve();
                                ListRow dayRow = new ListRow(new HeaderItem("Past 24 Hours", null), dayAdapter);
                                mRowsAdapter.add(0, dayRow);
                            }

                        } else {
                            // no recordings
                            if (nearTimers.size() > 0) {
                                ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                scheduledAdapter.Retrieve();
                                ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours", null), scheduledAdapter);
                                mRowsAdapter.add(0, scheduleRow);
                            } else {
                                mTitle.setText(R.string.lbl_no_recordings);

                            }
                        }
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(mApplication, exception.getLocalizedMessage());
            }
        });

    }
}
