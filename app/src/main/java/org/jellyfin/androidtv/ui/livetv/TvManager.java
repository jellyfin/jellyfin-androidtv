package org.jellyfin.androidtv.ui.livetv;

import static org.koin.java.KoinJavaComponent.get;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.preference.LiveTvPreferences;
import org.jellyfin.androidtv.preference.SystemPreferences;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.LiveTvChannelQuery;
import org.jellyfin.apiclient.model.livetv.ProgramQuery;
import org.jellyfin.apiclient.model.livetv.TimerInfoDto;
import org.jellyfin.apiclient.model.livetv.TimerQuery;
import org.jellyfin.apiclient.model.querying.ItemSortBy;
import org.jellyfin.apiclient.model.querying.ItemsResult;
import org.jellyfin.apiclient.model.results.ChannelInfoDtoResult;
import org.jellyfin.apiclient.model.results.TimerInfoDtoResult;
import org.koin.java.KoinJavaComponent;

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

import timber.log.Timber;

public class TvManager {
    private static List<ChannelInfoDto> allChannels;
    private static String[] channelIds;
    private static HashMap<String, ArrayList<BaseItemDto>> mProgramsDict = new HashMap<>();
    private static Calendar needLoadTime;
    private static Calendar programNeedLoadTime;
    private static boolean forceReload;

    public static String getLastLiveTvChannel() {
        return KoinJavaComponent.<SystemPreferences>get(SystemPreferences.class).get(SystemPreferences.Companion.getLiveTvLastChannel());
    }

    public static void setLastLiveTvChannel(String id) {
        SystemPreferences systemPreferences = KoinJavaComponent.<SystemPreferences>get(SystemPreferences.class);
        systemPreferences.set(SystemPreferences.Companion.getLiveTvPrevChannel(), systemPreferences.get(SystemPreferences.Companion.getLiveTvLastChannel()));
        systemPreferences.set(SystemPreferences.Companion.getLiveTvLastChannel(), id);
        updateLastPlayedDate(id);
        sortChannels();
    }

    public static String getPrevLiveTvChannel() {
        return KoinJavaComponent.<SystemPreferences>get(SystemPreferences.class).get(SystemPreferences.Companion.getLiveTvPrevChannel());
    }

    public static List<ChannelInfoDto> getAllChannels() {
        return allChannels;
    }

    public static void clearCache() {
        forceReload = true;
        allChannels = null;
    }

    public static void forceReload() {
        forceReload = true;
    }
    public static boolean shouldForceReload() { return forceReload; }

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
        //Get channels if needed
        if (allChannels != null && allChannels.size() > 0) {
            Timber.i("*** Channels already loaded - returning %d channels", allChannels.size());
            outerResponse.onResponse(sortChannels());
        } else {
            loadAllChannelsInternal(outerResponse);
        }

    }

    private static void loadAllChannelsInternal(final Response<Integer> outerResponse) {
        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setAddCurrentProgram(true);
        if (liveTvPreferences.get(LiveTvPreferences.Companion.getFavsAtTop())) query.setEnableFavoriteSorting(true);
        query.setSortOrder(ItemSortBy.DatePlayed.equals(liveTvPreferences.get(LiveTvPreferences.Companion.getChannelOrder())) ? SortOrder.Descending : null);
        query.setSortBy(new String[] {ItemSortBy.DatePlayed.equals(liveTvPreferences.get(LiveTvPreferences.Companion.getChannelOrder())) ? ItemSortBy.DatePlayed : ItemSortBy.SortName});
        Timber.d("*** About to load channels");
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(final ChannelInfoDtoResult response) {
                Timber.d("*** channel query response");
                allChannels = new ArrayList<>();
                if (response.getTotalRecordCount() > 0) {
                    Collections.addAll(allChannels, response.getItems());
                }

                outerResponse.onResponse(sortChannels());
            }
        });

    }

    private static int fillChannelIds() {
        int ndx = 0;
        channelIds = new String[allChannels.size()];
        String last = getLastLiveTvChannel();
        int i = 0;
        for (ChannelInfoDto channel : allChannels) {
            channelIds[i++] = channel.getId();
            if (channel.getId().equals(last)) ndx = i;
            //TvApp.getApplication().getLogger().Debug("Last played for "+channel.getName()+ " is "+channel.getUserData().getLastPlayedDate());
        }

        return ndx;
    }


    public static int sortChannels() {
        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        int ndx = 0;
        if (allChannels != null) {
            //Sort by last played - if selected
            if (ItemSortBy.DatePlayed.equals(liveTvPreferences.get(LiveTvPreferences.Companion.getChannelOrder()))) {
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

            if (liveTvPreferences.get(LiveTvPreferences.Companion.getFavsAtTop())) {
                Collections.sort(allChannels, new Comparator<ChannelInfoDto>() {
                    @Override
                    public int compare(ChannelInfoDto channelOne, ChannelInfoDto channelTwo) {
                        boolean channelOneFav = channelOne.getUserData() != null && channelOne.getUserData().getIsFavorite();
                        boolean channelTwoFav = channelTwo.getUserData() != null && channelTwo.getUserData().getIsFavorite();
                        return -Boolean.compare(channelOneFav, channelTwoFav);
                    }
                });
            }

            //And  fill in channel IDs
            ndx = fillChannelIds();
        }

        return ndx;
    }

    public static void getProgramsAsync(int startNdx, int endNdx, final Calendar start, Calendar endTime, final EmptyResponse outerResponse) {
        start.set(Calendar.MINUTE, start.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        start.set(Calendar.SECOND, 1);
        if (forceReload || needLoadTime == null || start.after(needLoadTime) || !mProgramsDict.containsKey(channelIds[startNdx]) || !mProgramsDict.containsKey(channelIds[endNdx])) {
            forceReload = false;
            ProgramQuery query = new ProgramQuery();
            query.setUserId(TvApp.getApplication().getCurrentUser().getId());
            endNdx = endNdx > channelIds.length ? channelIds.length : endNdx+1; //array copy range final ndx is exclusive
            query.setChannelIds(Arrays.copyOfRange(channelIds, startNdx, endNdx));
            query.setEnableImages(false);
            query.setSortBy(new String[] {ItemSortBy.StartDate});
            Calendar end = (Calendar) endTime.clone();
            end.setTimeZone(TimeZone.getTimeZone("Z"));
            end.add(Calendar.SECOND, -1);
            query.setMaxStartDate(end.getTime());
            query.setMinEndDate(start.getTime());

            Timber.d("*** About to get programs");

            KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvProgramsAsync(query, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    Timber.d("*** About to build dictionary");
                    buildProgramsDict(response.getItems(), start);
                    Timber.d("*** Programs retrieval finished");

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
            if (TimeUtils.convertToLocalDate(program.getEndDate()).getTime() > start)
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
        Date local = TimeUtils.convertToLocalDate(program.getStartDate());
        TextView on = new TextView(activity);
        on.setText(activity.getResources().getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(activity);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(activity.getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(activity);
        datetime.setText(TimeUtils.getFriendlyDate(activity, local)+ " @ "+android.text.format.DateFormat.getTimeFormat(activity).format(local)+ " ("+ DateUtils.getRelativeTimeSpanString(local.getTime())+")");
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

    public static void getScheduleRowsAsync(Context context, TimerQuery query, final Presenter presenter, final ArrayObjectAdapter rowAdapter, final Response<Integer> outerResponse) {
        KoinJavaComponent.<ApiClient>get(ApiClient.class).GetLiveTvTimersAsync(query, new Response<TimerInfoDtoResult>() {
            @Override
            public void onResponse(TimerInfoDtoResult response) {
                List<BaseItemDto> currentTimers = new ArrayList<>();
                //Get scheduled items and break out by day
                int currentDay = 0;
                for (TimerInfoDto timer : response.getItems()) {
                    int thisDay = getDayInt(TimeUtils.convertToLocalDate(timer.getStartDate()));
                    if (thisDay != currentDay) {
                        if (currentDay > 0 && currentTimers.size() > 0) {
                            //Add the last set of timers as a row
                            addRow(context, currentTimers, presenter, rowAdapter);
                            currentTimers.clear();
                        }
                        currentDay = thisDay;
                    }
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
                    currentTimers.add(programInfo);

                }

                if (currentTimers.size() > 0) addRow(context, currentTimers, presenter, rowAdapter);

                outerResponse.onResponse(rowAdapter.size());
            }

            @Override
            public void onError(Exception exception) {
                Utils.showToast(context, exception.getLocalizedMessage());
                outerResponse.onError(exception);
            }

        });

    }

    private static void addRow(Context context, List<BaseItemDto> timers, Presenter presenter, ArrayObjectAdapter rowAdapter) {
        ItemRowAdapter scheduledAdapter = new ItemRowAdapter(timers, presenter, rowAdapter, true);
        scheduledAdapter.Retrieve();
        ListRow scheduleRow = new ListRow(new HeaderItem(TimeUtils.getFriendlyDate(context, TimeUtils.convertToLocalDate(timers.get(0).getStartDate()), true)), scheduledAdapter);
        rowAdapter.add(scheduleRow);

    }

    private static int getDayInt(Date fulldate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fulldate);
        return cal.get(Calendar.DAY_OF_YEAR);
    }
}
