package tv.emby.embyatv.livetv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.results.ChannelInfoDtoResult;
import mediabrowser.model.results.ProgramInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.ui.GuideChannelHeader;
import tv.emby.embyatv.ui.GuidePagingButton;
import tv.emby.embyatv.ui.HorizontalScrollViewListener;
import tv.emby.embyatv.ui.ObservableHorizontalScrollView;
import tv.emby.embyatv.ui.ObservableScrollView;
import tv.emby.embyatv.ui.ProgramGridCell;
import tv.emby.embyatv.ui.ScrollViewListener;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 5/3/2015.
 */
public class LiveTvGuideActivity extends BaseActivity {

    public static final int ROW_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(),55);
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),6);
    private static final int IMAGE_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 150);
    public static final int PAGEBUTTON_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 20);
    public static final int PAGEBUTTON_WIDTH = 120 * PIXELS_PER_MINUTE;
    public static final int PAGE_SIZE = 50;

    private LiveTvGuideActivity mActivity;
    private LayoutInflater mInflater;
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

    private List<ChannelInfoDto> mAllChannels;

    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart;
    private long mCurrentLocalGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;

    private Handler mHandler = new Handler();

    private Typeface roboto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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


    }

    @Override
    protected void onResume() {
        super.onResume();

        fillTimeLine(12);
        loadAllChannels();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
        if (mDisplayChannelTask != null) mDisplayChannelTask.cancel(true);
    }

    private void loadAllChannels() {
        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                if (response.getTotalRecordCount() > 0) {
                    mAllChannels = new ArrayList<>();
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    //fake more channels
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    //
                    displayChannels(0, PAGE_SIZE);
                } else {
                    mAllChannels.clear();
                }
            }
        });

    }

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) end = mAllChannels.size();

        mCurrentDisplayChannelStartNdx = start;
        mCurrentDisplayChannelEndNdx = end - 1;
        if (mDisplayChannelTask != null) mDisplayChannelTask.cancel(true);
        mDisplayChannelTask  = new DisplayChannelTask();
        mDisplayChannelTask.execute(mAllChannels.subList(start, end).toArray());
    }

    private DisplayChannelTask mDisplayChannelTask;
    class DisplayChannelTask extends AsyncTask<Object[], Integer, Void> {

        @Override
        protected void onPreExecute() {
            mSpinner.setVisibility(View.VISIBLE);

            mChannels.removeAllViews();
            mProgramRows.removeAllViews();
        }

        @Override
        protected Void doInBackground(Object[]... params) {

            final Object[] channels = params[0];
            final String[] channelIds = new String[params[0].length];
            int i = 0;
            // Get channel ids
            for (Object item : params[0]) {
                ChannelInfoDto channel = (ChannelInfoDto) item;
                channelIds[i++] = (channel).getId();
                if (isCancelled()) return null;
            }

            //Load guide data for the given channels
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
                    if (isCancelled()) return;
                    if (response.getTotalRecordCount() > 0) {
                        if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
                        mDisplayProgramsTask = new DisplayProgramsTask();
                        mDisplayProgramsTask.execute(channels, response.getItems());
                    }
                }
            });

            return null;
        }

    }

    DisplayProgramsTask mDisplayProgramsTask;
    class DisplayProgramsTask extends AsyncTask<Object[], Integer, Void> {

        View firstRow;

        @Override
        protected void onPreExecute() {
            mChannels.removeAllViews();
            mProgramRows.removeAllViews();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) pageUpStart = 0;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);

                mProgramRows.addView(new GuidePagingButton(mActivity, pageUpStart, getString(R.string.lbl_load_channels)+mAllChannels.get(pageUpStart).getNumber() + " - "+mAllChannels.get(mCurrentDisplayChannelStartNdx-1).getNumber()));
            }


        }

        @Override
        protected Void doInBackground(Object[]... params) {
            ProgramInfoDto[] allPrograms = new ProgramInfoDto[params[1].length];
            for (int i = 0; i < params[1].length; i++) {
                allPrograms[i] = (ProgramInfoDto) params[1][i];
            }

            boolean first = true;

            for (Object item : params[0]) {
                if (isCancelled()) return null;
                ChannelInfoDto channel = (ChannelInfoDto) item;
                List<ProgramInfoDto> programs = getProgramsForChannel(channel.getId(), allPrograms);
                if (programs.size() > 0) {
                    final GuideChannelHeader header = new GuideChannelHeader(mActivity, channel);
                    final LinearLayout row = getProgramRow(programs);
                    if (first) {
                        first = false;
                        firstRow = row;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChannels.addView(header);
                            header.loadImage();
                            mProgramRows.addView(row);
                        }
                    });

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mCurrentDisplayChannelEndNdx < mAllChannels.size()-1) {
                // Show a paging row for channels below
                int pageDnEnd = mCurrentDisplayChannelEndNdx + PAGE_SIZE;
                if (pageDnEnd >= mAllChannels.size()) pageDnEnd = mAllChannels.size()-1;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);

                mProgramRows.addView(new GuidePagingButton(mActivity, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels)+mAllChannels.get(mCurrentDisplayChannelEndNdx+1).getNumber() + " - "+mAllChannels.get(pageDnEnd).getNumber()));
            }

            mSpinner.setVisibility(View.GONE);
            if (firstRow != null) firstRow.requestFocus();

        }
    }

    private LinearLayout getProgramRow(List<ProgramInfoDto> programs) {

        LinearLayout programRow = new LinearLayout(this);

        if (programs.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText("  <No Program Data Available>");
            empty.setGravity(Gravity.CENTER);
            empty.setHeight(ROW_HEIGHT);
            programRow.addView(empty);
            return programRow;
        }

        for (ProgramInfoDto item : programs) {
            long start = item.getStartDate() != null ? Utils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) start = getCurrentLocalStartDate();
            long end = item.getEndDate() != null ? Utils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            Long duration = (end - start) / 60000;
            //TvApp.getApplication().getLogger().Debug("Duration for "+item.getName()+" is "+duration.intValue());
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(this, item);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * PIXELS_PER_MINUTE, ROW_HEIGHT));
                program.setFocusable(true);

                programRow.addView(program);

            }

        }

        return programRow;
    }

    private void fillTimeLine(int hours) {
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
        mCurrentGuideEnd.add(Calendar.HOUR, hours);
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
