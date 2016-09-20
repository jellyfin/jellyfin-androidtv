package tv.emby.embyatv.browsing;

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
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.util.Utils;

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
        mCardPresenter = new CardPresenter();
        final TimerQuery scheduled = new TimerQuery();
        TvApp.getApplication().getApiClient().GetLiveTvTimersAsync(scheduled, new Response<TimerInfoDtoResult>() {
            @Override
            public void onResponse(TimerInfoDtoResult response) {
                List<BaseItemDto> currentTimers = new ArrayList<>();
                //Get scheduled items and break out by day
                int currentDay = 0;
                for (TimerInfoDto timer : response.getItems()) {
                    int thisDay = getDayInt(Utils.convertToLocalDate(timer.getStartDate()));
                    if (thisDay != currentDay) {
                        if (currentDay > 0 && currentTimers.size() > 0) {
                            //Add the last set of timers as a row
                            addRow(currentTimers);
                            currentTimers.clear();
                        }
                        currentDay = thisDay;
                    }
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
                    currentTimers.add(programInfo);

                }

                if (currentTimers.size() > 0) addRow(currentTimers);

                if (mRowsAdapter.size() == 0) mActivity.setTitle("No Scheduled Recordings");
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(mApplication, exception.getLocalizedMessage());
            }

        });

    }

    private void addRow(List<BaseItemDto> timers) {
        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(timers, mCardPresenter, mRowsAdapter, true);
        scheduledAdapter.Retrieve();
        ListRow scheduleRow = new ListRow(new HeaderItem(Utils.getFriendlyDate(Utils.convertToLocalDate(timers.get(0).getStartDate()), true)), scheduledAdapter);
        mRowsAdapter.add(scheduleRow);

    }

    private int getDayInt(Date fulldate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fulldate);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

}
