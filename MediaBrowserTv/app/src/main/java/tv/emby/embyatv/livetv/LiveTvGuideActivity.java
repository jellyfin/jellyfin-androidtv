package tv.emby.embyatv.livetv;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.results.ProgramInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.ui.HorizontalScrollViewListener;
import tv.emby.embyatv.ui.ObservableHorizontalScrollView;
import tv.emby.embyatv.ui.ObservableScrollView;
import tv.emby.embyatv.ui.ScrollViewListener;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/3/2015.
 */
public class LiveTvGuideActivity extends BaseActivity implements INotifyChannelsLoaded{

    public static final int ROW_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(),55);
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),7);

    private TextView mDisplayDate;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ScrollView mChannelScroller;
    private TextClock mClock;
    private HorizontalScrollView mTimelineScroller;

    private ChannelListAdapter mChannelAdapter;
    private ProgramListAdapter mProgramsAdapter;

    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart;
    private long mCurrentLocalGuideEnd;

    private Typeface roboto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        setContentView(R.layout.live_tv_guide);

        mDisplayDate = (TextView) findViewById(R.id.displayDate);
        mChannels = (LinearLayout) findViewById(R.id.channels);
        mTimeline = (LinearLayout) findViewById(R.id.timeline);
        mProgramRows = (LinearLayout) findViewById(R.id.programRows);
        mClock = (TextClock) findViewById(R.id.clock);
        mClock.setTypeface(roboto);

        mProgramRows.setFocusable(false);
        mChannelScroller = (ScrollView) findViewById(R.id.channelScroller);
        ObservableScrollView programVScroller = (ObservableScrollView) findViewById(R.id.programVScroller);
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x, y);
            }
        });

        mTimelineScroller = (HorizontalScrollView) findViewById(R.id.timelineHScroller);
        mTimelineScroller.setFocusable(false);
        mTimelineScroller.setFocusableInTouchMode(false);
        mTimeline.setFocusable(false);
        mTimeline.setFocusableInTouchMode(false);
        mChannelScroller.setFocusable(false);
        mChannelScroller.setFocusableInTouchMode(false);
        ObservableHorizontalScrollView programHScroller = (ObservableHorizontalScrollView) findViewById(R.id.programHScroller);
        programHScroller.setScrollViewListener(new HorizontalScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableHorizontalScrollView scrollView, int x, int y, int oldx, int oldy) {
                mTimelineScroller.scrollTo(x, y);
            }
        });

        programHScroller.setFocusable(false);
        programHScroller.setFocusableInTouchMode(false);

        mChannels.setFocusable(false);
        mChannelScroller.setFocusable(false);

        fillTimeLine();

        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        query.setLimit(50);
        mChannelAdapter = new ChannelListAdapter(this, this, mChannels, query);
        mChannelAdapter.Retrieve();

        mProgramsAdapter = new ProgramListAdapter(this, mProgramRows);

    }

    private void fillTimeLine() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        mCurrentLocalGuideStart = start.getTimeInMillis();

        mDisplayDate.setText(android.text.format.DateFormat.getDateFormat(this).format(start.getTime()));
        Calendar current = (Calendar) start.clone();
        mCurrentGuideEnd = (Calendar) start.clone();
        int oneHour = 60 * PIXELS_PER_MINUTE;
        mCurrentGuideEnd.add(Calendar.HOUR, 12);
        mCurrentLocalGuideEnd = mCurrentGuideEnd.getTimeInMillis();
        while (current.before(mCurrentGuideEnd)) {
            TextView time = new TextView(this);
            time.setText(android.text.format.DateFormat.getTimeFormat(this).format(current.getTime()));
            time.setWidth(oneHour);
            mTimeline.addView(time);
            current.add(Calendar.HOUR_OF_DAY, 1);
        }

    }

    @Override
    public void notifyChannelsLoaded(final int start, final String[] channelIds) {
        //Load guide data for the given channels starting at the given index
        ProgramQuery query = new ProgramQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setChannelIds(channelIds);
        Calendar end = (Calendar) mCurrentGuideEnd.clone();
        end.setTimeZone(TimeZone.getTimeZone("Z"));
        query.setMaxStartDate(end.getTime());
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("Z"));
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        query.setMinEndDate(now.getTime());

        TvApp.getApplication().getApiClient().GetLiveTvProgramsAsync(query, new Response<ProgramInfoDtoResult>() {
            @Override
            public void onResponse(ProgramInfoDtoResult response) {
                if (response.getTotalRecordCount() > 0) {
                    int i = start;
                    for (String id : channelIds) {
                        mProgramsAdapter.addRow(i++, getProgramsForChannel(id, response.getItems()));
                    }
                    mProgramRows.requestFocus();
                }
            }
        });
    }

    private List<ProgramInfoDto> getProgramsForChannel(String channelId, ProgramInfoDto[] programs) {
        List<ProgramInfoDto> results = new ArrayList<>();
        for (ProgramInfoDto program : programs) {
            if (program.getChannelId().equals(channelId) && Utils.convertToLocalDate(program.getEndDate()).getTime() > mCurrentLocalGuideStart) results.add(program);
        }
        return results;
    }

    public long getCurrentLocalStartDate() { return mCurrentLocalGuideStart; }
    public long getCurrentLocalEndDate() { return mCurrentLocalGuideEnd; }
}
