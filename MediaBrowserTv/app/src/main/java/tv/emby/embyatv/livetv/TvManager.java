package tv.emby.embyatv.livetv;

import android.app.Activity;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.results.ChannelInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.ui.ProgramGridCell;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 9/4/2015.
 */
public class TvManager {
    private static List<ChannelInfoDto> allChannels;
    private static String[] channelIds;
    private static HashMap<String, ArrayList<BaseItemDto>> mProgramsDict = new HashMap<>();
    private static Calendar needLoadTime;
    private static Calendar programNeedLoadTime;

    public static String getLastLiveTvChannel() {
        return TvApp.getApplication().getSystemPrefs().getString("sys_pref_last_tv_channel", null);
    }

    public static void setLastLiveTvChannel(String id) {
        TvApp.getApplication().getSystemPrefs().edit().putString("sys_pref_prev_tv_channel", TvApp.getApplication().getSystemPrefs().getString("sys_pref_last_tv_channel", null)).commit();
        TvApp.getApplication().getSystemPrefs().edit().putString("sys_pref_last_tv_channel", id).commit();
        updateLastPlayedDate(id);
        sortChannels();
    }

    public static String getPrevLiveTvChannel() {
        return TvApp.getApplication().getSystemPrefs().getString("sys_pref_prev_tv_channel", null);
    }

    public static List<ChannelInfoDto> getAllChannels() {
        return allChannels;
    }

    public static void resetChannels() {
        allChannels = null;
    }

    public static int getAllChannelsIndex(String id) {
        for (int i = 0; i < allChannels.size(); i++) {
            if (allChannels.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    public static ChannelInfoDto getChannel(int ndx) {
        return allChannels.get(ndx);
    }

    public static void updateLastPlayedDate(String channelId) {
        if (allChannels != null) {
            int ndx = getAllChannelsIndex(channelId);
            if (ndx >= 0) {
                TimeZone timeZone = Calendar.getInstance().getTimeZone();
                Date now = new Date();
                allChannels.get(ndx).getUserData().setLastPlayedDate(new Date(now.getTime()-timeZone.getRawOffset()));
            }
        }
    }

    public static void loadAllChannels(final Response<Integer> outerResponse) {
        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        query.setAddCurrentProgram(false);
        TvApp.getApplication().getLogger().Debug("*** About to load channels");
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(final ChannelInfoDtoResult response) {
                TvApp.getApplication().getLogger().Debug("*** channel query response");
                allChannels = new ArrayList<>();
                int ndx = 0;
                if (response.getTotalRecordCount() > 0) {
                    Collections.addAll(allChannels, response.getItems());
                }

                outerResponse.onResponse(sortChannels());
            }
        });

    }

    public static int sortChannels() {
        int ndx = 0;
        if (allChannels != null) {
            //Sort by last played - if selected
            if (TvApp.getApplication().getPrefs().getBoolean("pref_guide_sort_date", true)) {
                Collections.sort(allChannels, Collections.reverseOrder(new Comparator<ChannelInfoDto>() {
                    @Override
                    public int compare(ChannelInfoDto lhs, ChannelInfoDto rhs) {
                        long left = lhs.getUserData() == null || lhs.getUserData().getLastPlayedDate() == null ? 0 : lhs.getUserData().getLastPlayedDate().getTime();
                        long right = rhs.getUserData() == null || rhs.getUserData().getLastPlayedDate() == null ? 0 : rhs.getUserData().getLastPlayedDate().getTime();

                        long result = left - right;
                        return result == 0 ? 0 : result > 0 ? 1 : -1;
                    }
                }));
            }


            //And  fill in channel IDs
            channelIds = new String[allChannels.size()];
            String last = getLastLiveTvChannel();
            int i = 0;
            for (ChannelInfoDto channel : allChannels) {
                channelIds[i++] = channel.getId();
                if (channel.getId().equals(last)) ndx = i;
                //TvApp.getApplication().getLogger().Debug("Last played for "+channel.getName()+ " is "+channel.getUserData().getLastPlayedDate());
            }
        }

        return ndx;
    }

    public static void getProgramsAsync(int startNdx, int endNdx, Calendar endTime, final EmptyResponse outerResponse) {
        final Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Z"));
        start.set(Calendar.MINUTE, start.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        start.set(Calendar.SECOND, 1);
        if (needLoadTime == null || start.after(needLoadTime) || !mProgramsDict.containsKey(channelIds[startNdx]) || !mProgramsDict.containsKey(channelIds[endNdx])) {
            ProgramQuery query = new ProgramQuery();
            query.setUserId(TvApp.getApplication().getCurrentUser().getId());
            endNdx = endNdx > channelIds.length ? channelIds.length : endNdx+1; //array copy range final ndx is exclusive
            query.setChannelIds(Arrays.copyOfRange(channelIds, startNdx, endNdx));
            query.setEnableImages(false);
            query.setSortBy(new String[] {"StartDate"});
            Calendar end = (Calendar) endTime.clone();
            end.setTimeZone(TimeZone.getTimeZone("Z"));
            end.add(Calendar.SECOND, -1);
            query.setMaxStartDate(end.getTime());
            query.setMinEndDate(start.getTime());

            TvApp.getApplication().getLogger().Debug("*** About to get programs");

            TvApp.getApplication().getApiClient().GetLiveTvProgramsAsync(query, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    TvApp.getApplication().getLogger().Debug("*** About to build dictionary");
                    buildProgramsDict(response.getItems(), start);
                    TvApp.getApplication().getLogger().Debug("*** Programs retrieval finished");

                    outerResponse.onResponse();
                }

                @Override
                public void onError(Exception exception) {
                    outerResponse.onError(exception);
                }
            });

        } else {
            outerResponse.onResponse();
        }
    }

    private static void buildProgramsDict(BaseItemDto[] programs, Calendar startTime) {
        mProgramsDict = new HashMap<>();
        long start = startTime.getTimeInMillis();
        for (BaseItemDto program : programs) {
            String id = program.getChannelId();
            if (!mProgramsDict.containsKey(id)) mProgramsDict.put(id, new ArrayList<BaseItemDto>());
            if (Utils.convertToLocalDate(program.getEndDate()).getTime() > start)
                mProgramsDict.get(id).add(program);
        }
        needLoadTime = (Calendar) startTime.clone();
        needLoadTime.setTimeZone(TimeZone.getTimeZone("Z"));
        needLoadTime.add(Calendar.MINUTE, 29);

    }

    public static Calendar updateProgramsNeedsLoadTime() {
        programNeedLoadTime = new GregorianCalendar(TimeZone.getTimeZone("Z"));
        programNeedLoadTime.set(Calendar.MINUTE, programNeedLoadTime.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        programNeedLoadTime.set(Calendar.SECOND, 0);
        return programNeedLoadTime;
    }

    public static boolean programsNeedLoad(Calendar now) { return programNeedLoadTime == null || now.after(programNeedLoadTime); }

    public static List<BaseItemDto> getProgramsForChannel(String channelId, GuideFilters filters) {
        if (!mProgramsDict.containsKey(channelId)) return new ArrayList<>();

        List<BaseItemDto> results = mProgramsDict.get(channelId);
        boolean passes = filters == null || !filters.any();
        if (passes) return results;

        // There are filters - check them
        for (BaseItemDto program : results) {
            passes |= filters.passesFilter(program);
        }

        return passes ? results : new ArrayList<BaseItemDto>();
    }

    public static List<BaseItemDto> getProgramsForChannel(String channelId) {
        return !mProgramsDict.containsKey(channelId) ? new ArrayList<BaseItemDto>() : mProgramsDict.get(channelId);

    }

    public static void setTimelineRow(Activity activity, LinearLayout timelineRow, BaseItemDto program) {
        timelineRow.removeAllViews();
        Date local = Utils.convertToLocalDate(program.getStartDate());
        TextView on = new TextView(activity);
        on.setText(activity.getResources().getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(activity);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(activity.getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(activity);
        datetime.setText(Utils.getFriendlyDate(local)+ " @ "+android.text.format.DateFormat.getTimeFormat(activity).format(local)+ " ("+ DateUtils.getRelativeTimeSpanString(local.getTime())+")");
        timelineRow.addView(datetime);

    }

    // this makes focus movements more predictable for the grid view
    public static void setFocusParms(LinearLayout currentRow, LinearLayout otherRow, boolean up) {

        for (int currentRowNdx = 0; currentRowNdx < currentRow.getChildCount(); currentRowNdx++) {
            ProgramGridCell cell = (ProgramGridCell) currentRow.getChildAt(currentRowNdx);
            ProgramGridCell otherCell = getOtherCell(otherRow, cell);
            if (otherCell != null) {
                if (up) {
                    cell.setNextFocusUpId(otherCell.getId());
                    //TvApp.getApplication().getLogger().Debug("Setting up focus for " + cell.getProgram().getName() + " to " + otherCell.getProgram().getName()+"("+otherCell.getId()+")");
                } else {
                    cell.setNextFocusDownId(otherCell.getId());
                    //TvApp.getApplication().getLogger().Debug("Setting down focus for " + cell.getProgram().getName() + " to " + otherCell.getProgram().getName());
                }
            }
        }
    }

    private static ProgramGridCell getOtherCell(LinearLayout otherRow, ProgramGridCell cell) {
        // find first cell in other row where our left edge is within its body (will be first one who's right edge is greater than our left)
        for (int otherRowNdx = 0; otherRowNdx < otherRow.getChildCount(); otherRowNdx++) {
            ProgramGridCell otherCell = (ProgramGridCell) otherRow.getChildAt(otherRowNdx);
            if (otherCell.getProgram().getEndDate() != null && cell.getProgram().getStartDate() != null &&
                    otherCell.getProgram().getEndDate().getTime() > cell.getProgram().getStartDate().getTime()) {
                return otherCell;
            }
        }
        return null;
    }




}
