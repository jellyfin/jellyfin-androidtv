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
                            if (Utils.convertToLocalDate(timer.getStartDate()).getTime() <= next24) {
                                BaseItemDto programInfo = timer.getProgramInfo();
                                if (programInfo == null) {
                                    programInfo = new BaseItemDto();
                                    programInfo.setId(timer.getId());
                                    programInfo.setChannelName(timer.getChannelName());
                                    programInfo.setName(Utils.NullCoalesce(timer.getName(),"Unknown"));
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

                        if (recordingsResponse.getTotalRecordCount() > 0) {
                            List<BaseItemDto> dayItems = new ArrayList<>();
                            List<BaseItemDto> weekItems = new ArrayList<>();

                            long past24 = System.currentTimeMillis() - ticks24;
                            long pastWeek = System.currentTimeMillis() - (ticks24 * 7);
                            for (BaseItemDto item : recordingsResponse.getItems()) {
                                if (item.getDateCreated() != null) {
                                    if (Utils.convertToLocalDate(item.getDateCreated()).getTime() >= past24) {
                                        dayItems.add(item);
                                    } else if (Utils.convertToLocalDate(item.getDateCreated()).getTime() >= pastWeek) {
                                        weekItems.add(item);
                                    }
                                }
                            }

                            //First put all recordings in and retrieve
                            //All Recordings
                            RecordingQuery recordings = new RecordingQuery();
                            recordings.setFields(new ItemFields[]{ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                            recordings.setUserId(TvApp.getApplication().getCurrentUser().getId());
                            recordings.setEnableImages(true);
                            mRows.add(new BrowseRowDef("Recent Recordings", recordings, 50));
                            //All Recordings by group - will only be there for non-internal TV
                            RecordingGroupQuery recordingGroups = new RecordingGroupQuery();
                            recordingGroups.setUserId(TvApp.getApplication().getCurrentUser().getId());
                            mRows.add(new BrowseRowDef("All Recordings", recordingGroups));
                            rowLoader.loadRows(mRows);

                            //Now insert our smart rows
                            if (weekItems.size() > 0) {
                                ItemRowAdapter weekAdapter = new ItemRowAdapter(weekItems, mCardPresenter, mRowsAdapter, true);
                                weekAdapter.Retrieve();
                                ListRow weekRow = new ListRow(new HeaderItem("Past Week"), weekAdapter);
                                mRowsAdapter.add(0, weekRow);
                            }
                            if (nearTimers.size() > 0) {
                                ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                scheduledAdapter.Retrieve();
                                ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
                                mRowsAdapter.add(0, scheduleRow);
                            }
                            if (dayItems.size() > 0) {
                                ItemRowAdapter dayAdapter = new ItemRowAdapter(dayItems, mCardPresenter, mRowsAdapter, true);
                                dayAdapter.Retrieve();
                                ListRow dayRow = new ListRow(new HeaderItem("Past 24 Hours"), dayAdapter);
                                mRowsAdapter.add(0, dayRow);
                            }

                        } else {
                            // no recordings
                            if (nearTimers.size() > 0) {
                                ItemRowAdapter scheduledAdapter = new ItemRowAdapter(nearTimers, mCardPresenter, mRowsAdapter, true);
                                scheduledAdapter.Retrieve();
                                ListRow scheduleRow = new ListRow(new HeaderItem("Scheduled in Next 24 Hours"), scheduledAdapter);
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
