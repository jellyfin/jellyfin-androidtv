package tv.emby.embyatv.livetv;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/3/2015.
 */
public class LiveTvGuideActivity extends BaseActivity implements INotifyChannelsLoaded{

    public static final int ROW_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(),55);
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),6);
    private static final int IMAGE_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 150);
    private static final int BACKDROP_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 2000);

    private Activity mActivity;
    private TextView mDisplayDate;
    private TextView mTitle;
    private TextView mSummary;
    private ImageView mImage;
    private ImageView mBackdrop;
    private LinearLayout mInfoRow;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ScrollView mChannelScroller;
    private TextClock mClock;
    private HorizontalScrollView mTimelineScroller;
    private View mSpinner;

    private ProgramInfoDto mSelectedProgram;

    private ChannelListAdapter mChannelAdapter;
    private ProgramListAdapter mProgramsAdapter;

    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart;
    private long mCurrentLocalGuideEnd;

    private Handler mHandler = new Handler();

    private Typeface roboto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        setContentView(R.layout.live_tv_guide);

        mDisplayDate = (TextView) findViewById(R.id.displayDate);
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setTypeface(roboto);
        mSummary = (TextView) findViewById(R.id.summary);
        mSummary.setTypeface(roboto);
        mInfoRow = (LinearLayout) findViewById(R.id.infoRow);
        mImage = (ImageView) findViewById(R.id.programImage);
        mBackdrop = (ImageView) findViewById(R.id.backdrop);
        mChannels = (LinearLayout) findViewById(R.id.channels);
        mTimeline = (LinearLayout) findViewById(R.id.timeline);
        mProgramRows = (LinearLayout) findViewById(R.id.programRows);
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setVisibility(View.VISIBLE);
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
        start.set(Calendar.MINUTE, start.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        mCurrentLocalGuideStart = start.getTimeInMillis();

        mDisplayDate.setText(android.text.format.DateFormat.getDateFormat(this).format(start.getTime()));
        Calendar current = (Calendar) start.clone();
        mCurrentGuideEnd = (Calendar) start.clone();
        int oneHour = 60 * PIXELS_PER_MINUTE;
        int halfHour = 30 * PIXELS_PER_MINUTE;
        int interval = current.get(Calendar.MINUTE) >= 30 ? 30 : 60;
        mCurrentGuideEnd.add(Calendar.HOUR, 12);
        mCurrentLocalGuideEnd = mCurrentGuideEnd.getTimeInMillis();
        while (current.before(mCurrentGuideEnd)) {
            TextView time = new TextView(this);
            time.setText(android.text.format.DateFormat.getTimeFormat(this).format(current.getTime()));
            time.setWidth(interval == 30 ? halfHour : oneHour);
            mTimeline.addView(time);
            current.add(Calendar.MINUTE, interval);
            //after first one, we always go on hours
            interval = 60;
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
        now.set(Calendar.MINUTE, now.get(Calendar.MINUTE) >= 30 ? 30 : 0);
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
                    mSpinner.setVisibility(View.GONE);
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

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            mTitle.setText(mSelectedProgram.getName());
            mSummary.setText(mSelectedProgram.getOverview());
            String url = Utils.getPrimaryImageUrl(mSelectedProgram, TvApp.getApplication().getApiClient());
            //url = "https://image.tmdb.org/t/p/w396/zr2p353wrd6j3wjLgDT4TcaestB.jpg";
            Picasso.with(mActivity).load(url).resize(IMAGE_SIZE, IMAGE_SIZE).centerInside().into(mImage);

            mInfoRow.removeAllViews();
            // fake
//            mSelectedProgram.setCommunityRating(7.5f);
//            InfoLayoutHelper.addCriticInfo(mActivity, mSelectedProgram, mInfoRow);
//            InfoLayoutHelper.addSpacer(mActivity, mInfoRow, " 1983  ", 14);
//            InfoLayoutHelper.addBlockText(mActivity, mInfoRow, "R", 12);
//            InfoLayoutHelper.addSpacer(mActivity, mInfoRow, "  ", 10);
            //

            if (mSelectedProgram.getIsNews()) {
                mBackdrop.setImageResource(R.drawable.newsbanner);

            } else if (mSelectedProgram.getIsKids()) {
                mBackdrop.setImageResource(R.drawable.kidsbanner);

            } else if (mSelectedProgram.getIsSports()) {
                mBackdrop.setImageResource(R.drawable.sportsbanner);

            } else if (mSelectedProgram.getIsMovie()) {
                mBackdrop.setImageResource(R.drawable.moviebanner);

            } else {
                mBackdrop.setImageResource(R.drawable.tvbanner);
            }
        }
    };

    public void setSelectedProgram(ProgramInfoDto program) {
        mSelectedProgram = program;
        mHandler.removeCallbacks(detailUpdateTask);
        mHandler.postDelayed(detailUpdateTask, 500);
    }
}
