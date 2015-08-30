package tv.emby.embyatv.livetv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.results.ChannelInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.ui.GuideChannelHeader;
import tv.emby.embyatv.ui.GuidePagingButton;
import tv.emby.embyatv.ui.HorizontalScrollViewListener;
import tv.emby.embyatv.ui.ObservableHorizontalScrollView;
import tv.emby.embyatv.ui.ObservableScrollView;
import tv.emby.embyatv.ui.ProgramGridCell;
import tv.emby.embyatv.ui.RecordPopup;
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
    public static final int NORMAL_HOURS = 12;
    public static final int FILTERED_HOURS = 3;

    private LiveTvGuideActivity mActivity;
    private TextView mDisplayDate;
    private TextView mTitle;
    private TextView mChannelStatus;
    private TextView mFilterStatus;
    private TextView mSummary;
    private ImageView mImage;
    private ImageView mBackdrop;
    private LinearLayout mInfoRow;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ScrollView mChannelScroller;
    private HorizontalScrollView mTimelineScroller;
    private View mSpinner;

    private BaseItemDto mSelectedProgram;
    private ProgramGridCell mSelectedProgramView;
    private long mLastLoad = 0;

    private List<ChannelInfoDto> mAllChannels;
    private HashMap<String, ArrayList<BaseItemDto>> mProgramsDict = new HashMap<>();
    private String mFirstFocusChannelId;
    private GuideFilters mFilters = new GuideFilters();

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
        roboto = TvApp.getApplication().getDefaultFont();

        setContentView(R.layout.live_tv_guide);

        mDisplayDate = (TextView) findViewById(R.id.displayDate);
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setTypeface(roboto);
        mSummary = (TextView) findViewById(R.id.summary);
        mSummary.setTypeface(roboto);
        mChannelStatus = (TextView) findViewById(R.id.channelsStatus);
        mFilterStatus = (TextView) findViewById(R.id.filterStatus);
        mChannelStatus.setTypeface(roboto);
        mFilterStatus.setTypeface(roboto);
        mChannelStatus.setTextColor(Color.GRAY);
        mFilterStatus.setTextColor(Color.GRAY);
        mInfoRow = (LinearLayout) findViewById(R.id.infoRow);
        mImage = (ImageView) findViewById(R.id.programImage);
        mBackdrop = (ImageView) findViewById(R.id.backdrop);
        mChannels = (LinearLayout) findViewById(R.id.channels);
        mTimeline = (LinearLayout) findViewById(R.id.timeline);
        mProgramRows = (LinearLayout) findViewById(R.id.programRows);
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setVisibility(View.VISIBLE);
        TextClock clock = (TextClock) findViewById(R.id.clock);
        clock.setTypeface(roboto);

        findViewById(R.id.filterButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterOptions();
            }
        });

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

        //Register to receive message from popup
        registerMessageListener(new IMessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                if (message.equals(CustomMessage.ActionComplete)) dismissProgramOptions();
            }
        });

    }

    private int getGuideHours() {
        return mFilters.any() ? FILTERED_HOURS : NORMAL_HOURS;
    }

    private void load() {
        fillTimeLine(getGuideHours());
        loadAllChannels();
        mLastLoad = System.currentTimeMillis();
    }

    private void reload() {
        fillTimeLine(getGuideHours());
        displayChannels(mCurrentDisplayChannelStartNdx, PAGE_SIZE);
        mLastLoad = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (System.currentTimeMillis() > mLastLoad + 3600000) if (mAllChannels == null) load(); else reload();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
        if (mDisplayChannelTask != null) mDisplayChannelTask.cancel(true);
        if (mDetailPopup != null) mDetailPopup.dismiss();
        if (mRecordPopup != null) mRecordPopup.dismiss();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                // bring up filter selection
                showFilterOptions();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if ((mDetailPopup == null || !mDetailPopup.isShowing()) && (mFilterPopup == null || !mFilterPopup.isShowing())
                        && mSelectedProgram != null && mSelectedProgram.getChannelId() != null) {
                    // tune to the current channel
                    Utils.Beep();
                    Utils.retrieveAndPlay(mSelectedProgram.getChannelId(), false, this);
                    return true;
                }
        }

        return super.onKeyUp(keyCode, event);
    }

    private DetailPopup mDetailPopup;
    class DetailPopup {
        final int MOVIE_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 540);
        final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);

        PopupWindow mPopup;
        LiveTvGuideActivity mActivity;
        TextView mDTitle;
        TextView mDSummary;
        TextView mDRecordInfo;
        LinearLayout mDTimeline;
        LinearLayout mDInfoRow;
        LinearLayout mDButtonRow;
        LinearLayout mDSimilarRow;
        Button mFirstButton;

        DetailPopup(LiveTvGuideActivity activity) {
            mActivity = activity;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.program_detail_popup, null);
            mPopup = new PopupWindow(layout, mSummary.getWidth(), NORMAL_HEIGHT);
            mPopup.setFocusable(true);
            mPopup.setOutsideTouchable(true);
            mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
            mPopup.setAnimationStyle(R.style.PopupSlideInTop);
            mDTitle = (TextView)layout.findViewById(R.id.title);
            mDTitle.setTypeface(roboto);
            mDSummary = (TextView)layout.findViewById(R.id.summary);
            mDSummary.setTypeface(roboto);
            mDRecordInfo = (TextView) layout.findViewById(R.id.recordLine);

            mDTimeline = (LinearLayout) layout.findViewById(R.id.timeline);
            mDButtonRow = (LinearLayout) layout.findViewById(R.id.buttonRow);
            mDInfoRow = (LinearLayout) layout.findViewById(R.id.infoRow);
            mDSimilarRow = (LinearLayout) layout.findViewById(R.id.similarRow);
        }

        public boolean isShowing() {
            return (mPopup != null && mPopup.isShowing());
        }

        public void setContent(final BaseItemDto program) {
            mDTitle.setText(program.getName());
            mDSummary.setText(program.getOverview());
            if (mDSummary.getLineCount() < 2) {
                mDSummary.setGravity(Gravity.CENTER);
            } else {
                mDSummary.setGravity(Gravity.LEFT);
            }
            //TvApp.getApplication().getLogger().Debug("Text height: "+mDSummary.getHeight() + " (120 = "+Utils.convertDpToPixel(mActivity, 120)+")");

            // build timeline info
            setTimelineRow(mDTimeline, program);

            //fake info row
//            mDInfoRow.removeAllViews();
//            InfoLayoutHelper.addCriticInfo(mActivity, program, mDInfoRow);
//            InfoLayoutHelper.addSpacer(mActivity, mDInfoRow, " 2003  ", 14);
//            InfoLayoutHelper.addBlockText(mActivity, mDInfoRow, "R", 12);
//            InfoLayoutHelper.addSpacer(mActivity, mDInfoRow, "  ", 10);
            //

            //buttons
            mFirstButton = null;
            mDButtonRow.removeAllViews();
            Date now = new Date();
            Date local = Utils.convertToLocalDate(program.getStartDate());
            if (Utils.convertToLocalDate(program.getEndDate()).getTime() > now.getTime()) {
                if (local.getTime() <= now.getTime()) {
                    // program in progress - tune first button
                    mFirstButton = createTuneButton();
                }

                if (TvApp.getApplication().getCurrentUser().getPolicy().getEnableLiveTvManagement()) {
                    if (program.getTimerId() != null) {
                        // cancel button
                        Button cancel = new Button(mActivity);
                        cancel.setText(getString(R.string.lbl_cancel_recording));
                        cancel.setTextColor(Color.WHITE);
                        cancel.setBackground(getResources().getDrawable(R.drawable.emby_button));
                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                TvApp.getApplication().getApiClient().CancelLiveTvTimerAsync(mSelectedProgram.getTimerId(), new EmptyResponse() {
                                    @Override
                                    public void onResponse() {
                                        mSelectedProgramView.setRecTimer(null);
                                        dismiss();
                                        Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                    }

                                    @Override
                                    public void onError(Exception ex) {
                                        Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                    }
                                });
                            }
                        });
                        mDButtonRow.addView(cancel);
                        if (mFirstButton == null) mFirstButton = cancel;
                        // recording info
                        mDRecordInfo.setText(local.getTime() <= now.getTime() ? getString(R.string.msg_recording_now) : getString(R.string.msg_will_record));
                    } else {
                        // record button
                        Button rec = new Button(mActivity);
                        rec.setText(getString(R.string.lbl_record));
                        rec.setTextColor(Color.WHITE);
                        rec.setBackground(getResources().getDrawable(R.drawable.emby_button));
                        rec.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showRecordingOptions(false);
                            }
                        });
                        mDButtonRow.addView(rec);
                        if (mFirstButton == null) mFirstButton = rec;
                        mDRecordInfo.setText("");
                    }
                    if (Utils.isTrue(program.getIsSeries())) {
                        if (program.getSeriesTimerId() != null) {
                            // cancel series button
                            Button cancel = new Button(mActivity);
                            cancel.setText(getString(R.string.lbl_cancel_series));
                            cancel.setTextColor(Color.WHITE);
                            cancel.setBackground(getResources().getDrawable(R.drawable.emby_button));
                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new AlertDialog.Builder(mActivity)
                                            .setTitle(getString(R.string.lbl_cancel_series))
                                            .setMessage(getString(R.string.msg_cancel_entire_series))
                                            .setNegativeButton(R.string.lbl_no, null)
                                            .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    TvApp.getApplication().getApiClient().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyResponse() {
                                                        @Override
                                                        public void onResponse() {
                                                            mSelectedProgramView.setRecSeriesTimer(null);
                                                            dismiss();
                                                            Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                                                        }

                                                        @Override
                                                        public void onError(Exception ex) {
                                                            Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                                                        }
                                                    });
                                                }
                                            }).show();
                                }
                            });
                            mDButtonRow.addView(cancel);
                        }else {
                            // record series button
                            Button rec = new Button(mActivity);
                            rec.setText(getString(R.string.lbl_record_series));
                            rec.setTextColor(Color.WHITE);
                            rec.setBackground(getResources().getDrawable(R.drawable.emby_button));
                            rec.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showRecordingOptions(true);
                                }
                            });
                            mDButtonRow.addView(rec);
                        }
                    }

                }

                if (local.getTime() > now.getTime()) {
                    // add tune to button for programs that haven't started yet
                    createTuneButton();
                }


            } else {
                // program has already ended
                mDRecordInfo.setText(getString(R.string.lbl_program_ended));
                mFirstButton = createTuneButton();
            }
//                if (program.getIsMovie()) {
//                    mDSimilarRow.setVisibility(View.VISIBLE);
//                    mPopup.setHeight(MOVIE_HEIGHT);
//                } else {
            mDSimilarRow.setVisibility(View.GONE);
//                    mPopup.setHeight(NORMAL_HEIGHT);
//
//                }
        }

        public Button createTuneButton() {
            Button tune = addButton(mDButtonRow, R.string.lbl_tune_to_channel);
            tune.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.retrieveAndPlay(mSelectedProgram.getChannelId(), false, mActivity);
                    mPopup.dismiss();
                }
            });

            return tune;
        }

        public void show() {
            mPopup.showAtLocation(mImage, Gravity.NO_GRAVITY, mTitle.getLeft(), mTitle.getTop() - 10);
            if (mFirstButton != null) mFirstButton.requestFocus();

        }

        public void dismiss() {
            if (mPopup != null && mPopup.isShowing()) {
                mPopup.dismiss();
            }
        }
    }

    private FilterPopup mFilterPopup;
    class FilterPopup {

        final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 250);
        final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);

        PopupWindow mPopup;
        LiveTvGuideActivity mActivity;
        CheckBox mMovies;
        CheckBox mNews;
        CheckBox mSeries;
        CheckBox mKids;
        CheckBox mSports;
        CheckBox mPremiere;

        Button mFilterButton;
        Button mClearButton;

        FilterPopup(LiveTvGuideActivity activity) {
            mActivity = activity;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.guide_filter_popup, null);
            mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
            mPopup.setFocusable(true);
            mPopup.setOutsideTouchable(true);
            mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
            mPopup.setAnimationStyle(R.style.PopupSlideInRight);
            mMovies = (CheckBox) layout.findViewById(R.id.movies);
            mSeries = (CheckBox) layout.findViewById(R.id.series);
            mNews = (CheckBox) layout.findViewById(R.id.news);
            mKids = (CheckBox) layout.findViewById(R.id.kids);
            mSports = (CheckBox) layout.findViewById(R.id.sports);
            mPremiere = (CheckBox) layout.findViewById(R.id.premiere);

            mFilterButton = (Button) layout.findViewById(R.id.okButton);
            mFilterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFilters.setMovies(mMovies.isChecked());
                    mFilters.setSeries(mSeries.isChecked());
                    mFilters.setNews(mNews.isChecked());
                    mFilters.setKids(mKids.isChecked());
                    mFilters.setSports(mSports.isChecked());
                    mFilters.setPremiere(mPremiere.isChecked());

                    load();
                    mPopup.dismiss();
                }
            });
            mClearButton = (Button) layout.findViewById(R.id.clearButton);
            mClearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFilters.clear();
                    load();
                    mPopup.dismiss();
                }
            });

        }

        public boolean isShowing() {
            return (mPopup != null && mPopup.isShowing());
        }

        public void show() {
            mMovies.setChecked(mFilters.isMovies());
            mSeries.setChecked(mFilters.isSeries());
            mNews.setChecked(mFilters.isNews());
            mKids.setChecked(mFilters.isKids());
            mSports.setChecked(mFilters.isSports());
            mPremiere.setChecked(mFilters.isPremiere());

            mPopup.showAtLocation(mTimelineScroller, Gravity.NO_GRAVITY, mTimelineScroller.getRight(), mSummary.getTop());

        }

        public void dismiss() {
            if (mPopup != null && mPopup.isShowing()) {
                mPopup.dismiss();
            }
        }
    }

    private RecordPopup mRecordPopup;

    private void setTimelineRow(LinearLayout timelineRow, BaseItemDto program) {
        timelineRow.removeAllViews();
        Date local = Utils.convertToLocalDate(program.getStartDate());
        TextView on = new TextView(mActivity);
        on.setText(getString(R.string.lbl_on));
        timelineRow.addView(on);
        TextView channel = new TextView(mActivity);
        channel.setText(program.getChannelName());
        channel.setTypeface(null, Typeface.BOLD);
        channel.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        timelineRow.addView(channel);
        TextView datetime = new TextView(mActivity);
        datetime.setText(Utils.getFriendlyDate(local)+ " @ "+android.text.format.DateFormat.getTimeFormat(mActivity).format(local)+ " ("+ DateUtils.getRelativeTimeSpanString(local.getTime())+")");
        timelineRow.addView(datetime);

    }

    public void showRecordingOptions(final boolean recordSeries) {
        if (mRecordPopup == null) mRecordPopup = new RecordPopup(this, mImage, mTitle.getLeft(), mTitle.getTop() - 10, mSummary.getWidth());
        TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mSelectedProgram.getId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                mRecordPopup.setContent(mSelectedProgram, response, mSelectedProgramView, recordSeries);
                mRecordPopup.show();
            }
        });
    }

    public void dismissProgramOptions() {
        if (mDetailPopup != null) mDetailPopup.dismiss();
    }
    public void showProgramOptions() {
        if (mSelectedProgram == null) return;

        if (mDetailPopup == null) mDetailPopup = new DetailPopup(this);
        mDetailPopup.setContent(mSelectedProgram);
        mDetailPopup.show();

    }

    public void showFilterOptions() {

        if (mFilterPopup == null) mFilterPopup = new FilterPopup(this);
        mFilterPopup.show();

    }

    private Button addButton(LinearLayout layout, int stringResource) {
        Button btn = new Button(this);
        btn.setText(getString(stringResource));
        btn.setTextColor(Color.WHITE);
        btn.setBackground(getResources().getDrawable(R.drawable.emby_button));
        layout.addView(btn);
        return btn;
    }

    private void loadAllChannels() {
        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        TvApp.getApplication().getLogger().Debug("*** About to load channels");
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
                TvApp.getApplication().getLogger().Debug("*** channel query response");
                mAllChannels = new ArrayList<>();
                if (response.getTotalRecordCount() > 0) {
                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    //fake more channels
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
//                    mAllChannels.addAll(Arrays.asList(response.getItems()));
                    //

                    mFirstFocusChannelId = TvApp.getApplication().getLastLiveTvChannel();
                    int ndx = 0;
                    if (mFirstFocusChannelId != null) {
                        ndx = getAllChannelsIndex(mFirstFocusChannelId);
                        if (ndx  >= PAGE_SIZE) {
                            // last channel is not in first page so grab a set where it will be in the middle
                            ndx = ndx - (PAGE_SIZE / 2);
                        } else {
                            ndx = 0; // just start at beginning
                        }
                    }

                    displayChannels(ndx, PAGE_SIZE);

                } else {
                    mSpinner.setVisibility(View.GONE);
                }
            }
        });

    }

    private int getAllChannelsIndex(String id) {
        for (int i = 0; i < mAllChannels.size(); i++) {
            if (mAllChannels.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) end = mAllChannels.size();

        mCurrentDisplayChannelStartNdx = start;
        mCurrentDisplayChannelEndNdx = end - 1;
        if (mDisplayChannelTask != null) mDisplayChannelTask.cancel(true);
        mDisplayChannelTask  = new DisplayChannelTask();
        // if we are filtered, then we need to get programs for all channels
        mDisplayChannelTask.execute(mFilters.any() ? mAllChannels.toArray() : mAllChannels.subList(start, end).toArray());
    }

    private DisplayChannelTask mDisplayChannelTask;
    class DisplayChannelTask extends AsyncTask<Object[], Integer, Void> {

        @Override
        protected void onPreExecute() {
            TvApp.getApplication().getLogger().Debug("*** Display channels pre-execute");
            mSpinner.setVisibility(View.VISIBLE);

            mChannels.removeAllViews();
            mProgramRows.removeAllViews();
            mChannelStatus.setText("");
            mFilterStatus.setText("");
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
            query.setFields(new ItemFields[] {ItemFields.Overview});
            Calendar end = (Calendar) mCurrentGuideEnd.clone();
            end.setTimeZone(TimeZone.getTimeZone("Z"));
            query.setMaxStartDate(end.getTime());
            Calendar now = new GregorianCalendar(TimeZone.getTimeZone("Z"));
            now.set(Calendar.MINUTE, now.get(Calendar.MINUTE) >= 30 ? 30 : 0);
            now.set(Calendar.SECOND, 0);
            query.setMinEndDate(now.getTime());

            TvApp.getApplication().getLogger().Debug("*** About to get programs");
            TvApp.getApplication().getApiClient().GetLiveTvProgramsAsync(query, new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    if (isCancelled()) return;
                    TvApp.getApplication().getLogger().Debug("*** Programs response");
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
        int displayedChannels = 0;

        @Override
        protected void onPreExecute() {
            TvApp.getApplication().getLogger().Debug("*** Display programs pre-execute");
            mChannels.removeAllViews();
            mProgramRows.removeAllViews();

            if (mCurrentDisplayChannelStartNdx > 0 && !mFilters.any()) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) pageUpStart = 0;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);
                displayedChannels = 0;

                mProgramRows.addView(new GuidePagingButton(mActivity, pageUpStart, getString(R.string.lbl_load_channels)+mAllChannels.get(pageUpStart).getNumber() + " - "+mAllChannels.get(mCurrentDisplayChannelStartNdx-1).getNumber()));
            }


        }

        @Override
        protected Void doInBackground(Object[]... params) {
            BaseItemDto[] allPrograms = new BaseItemDto[params[1].length];
            for (int i = 0; i < params[1].length; i++) {
                allPrograms[i] = (BaseItemDto) params[1][i];
            }

            buildProgramsDict(allPrograms);

            boolean first = true;

            TvApp.getApplication().getLogger().Debug("*** About to iterate programs");
            for (Object item : params[0]) {
                if (isCancelled()) return null;
                final ChannelInfoDto channel = (ChannelInfoDto) item;
                List<BaseItemDto> programs = getProgramsForChannel(channel.getId());
                if (programs.size() > 0) {
                    final LinearLayout row = getProgramRow(programs);
                    if (first) {
                        first = false;
                        firstRow = row;
                    }

                    // put focus on the last tuned channel
                    if (channel.getId().equals(mFirstFocusChannelId)) {
                        firstRow = row;
                        mFirstFocusChannelId = null; // only do this first time in not while paging around
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GuideChannelHeader header = new GuideChannelHeader(mActivity, channel);
                            mChannels.addView(header);
                            header.loadImage();
                            mProgramRows.addView(row);
                        }
                    });

                    displayedChannels++;

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TvApp.getApplication().getLogger().Debug("*** Display programs post execute");
            if (mCurrentDisplayChannelEndNdx < mAllChannels.size()-1 && !mFilters.any()) {
                // Show a paging row for channels below
                int pageDnEnd = mCurrentDisplayChannelEndNdx + PAGE_SIZE;
                if (pageDnEnd >= mAllChannels.size()) pageDnEnd = mAllChannels.size()-1;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);

                mProgramRows.addView(new GuidePagingButton(mActivity, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels)+mAllChannels.get(mCurrentDisplayChannelEndNdx+1).getNumber() + " - "+mAllChannels.get(pageDnEnd).getNumber()));
            }

            mChannelStatus.setText(displayedChannels+" of "+mAllChannels.size()+" channels");
            mFilterStatus.setText(mFilters.toString() + " for next "+getGuideHours()+" hours");
            mFilterStatus.setTextColor(mFilters.any() ? Color.WHITE : Color.GRAY);

            mSpinner.setVisibility(View.GONE);
            if (firstRow != null) firstRow.requestFocus();

        }
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs) {

        LinearLayout programRow = new LinearLayout(this);

        if (programs.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText("  <No Program Data Available>");
            empty.setGravity(Gravity.CENTER);
            empty.setHeight(ROW_HEIGHT);
            programRow.addView(empty);
            return programRow;
        }

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? Utils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) start = getCurrentLocalStartDate();
            if (start > prevEnd) {
                // fill empty time slot
                TextView empty = new TextView(this);
                empty.setText("  <No Program Data Available>");
                empty.setGravity(Gravity.CENTER);
                empty.setHeight(ROW_HEIGHT);
                Long duration = (start - prevEnd) / 60000;
                empty.setWidth(duration.intValue() * PIXELS_PER_MINUTE);
                programRow.addView(empty);
            }
            long end = item.getEndDate() != null ? Utils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
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

        mDisplayDate.setText(Utils.getFriendlyDate(start.getTime()));
        Calendar current = (Calendar) start.clone();
        mCurrentGuideEnd = (Calendar) start.clone();
        int oneHour = 60 * PIXELS_PER_MINUTE;
        int halfHour = 30 * PIXELS_PER_MINUTE;
        int interval = current.get(Calendar.MINUTE) >= 30 ? 30 : 60;
        mCurrentGuideEnd.add(Calendar.HOUR, hours);
        mCurrentLocalGuideEnd = mCurrentGuideEnd.getTimeInMillis();
        mTimeline.removeAllViews();
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

    private void buildProgramsDict(BaseItemDto[] programs) {
        mProgramsDict = new HashMap<>();
        for (BaseItemDto program : programs) {
            String id = program.getChannelId();
            if (!mProgramsDict.containsKey(id)) mProgramsDict.put(id, new ArrayList<BaseItemDto>());
            if (Utils.convertToLocalDate(program.getEndDate()).getTime() > mCurrentLocalGuideStart) mProgramsDict.get(id).add(program);
        }
    }

    private List<BaseItemDto> getProgramsForChannel(String channelId) {
        if (!mProgramsDict.containsKey(channelId)) return new ArrayList<>();

        List<BaseItemDto> results = mProgramsDict.get(channelId);
        boolean passes = !mFilters.any();
        if (passes) return results;

        // There are filters - check them
        for (BaseItemDto program : results) {
            passes |= mFilters.passesFilter(program);
        }

        return passes ? results : new ArrayList<BaseItemDto>();
    }


    public long getCurrentLocalStartDate() { return mCurrentLocalGuideStart; }
    public long getCurrentLocalEndDate() { return mCurrentLocalGuideEnd; }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            mTitle.setText(mSelectedProgram.getName());
            mSummary.setText(mSelectedProgram.getOverview());
            mDisplayDate.setText(Utils.getFriendlyDate(Utils.convertToLocalDate(mSelectedProgram.getStartDate())));
            String url = Utils.getPrimaryImageUrl(mSelectedProgram, TvApp.getApplication().getApiClient());
            Picasso.with(mActivity).load(url).resize(IMAGE_SIZE, IMAGE_SIZE).centerInside().into(mImage);

            mInfoRow.removeAllViews();
            // fake
//            mSelectedProgram.setCommunityRating(7.5f);
//            InfoLayoutHelper.addCriticInfo(mActivity, mSelectedProgram, mInfoRow);
//            InfoLayoutHelper.addSpacer(mActivity, mInfoRow, " 2003  ", 14);
//            InfoLayoutHelper.addBlockText(mActivity, mInfoRow, "R", 12);
//            InfoLayoutHelper.addSpacer(mActivity, mInfoRow, "  ", 10);
            //

            if (Utils.isTrue(mSelectedProgram.getIsNews())) {
                mBackdrop.setImageResource(R.drawable.newsbanner);

            } else if (Utils.isTrue(mSelectedProgram.getIsKids())) {
                mBackdrop.setImageResource(R.drawable.kidsbanner);

            } else if (Utils.isTrue(mSelectedProgram.getIsSports())) {
                mBackdrop.setImageResource(R.drawable.sportsbanner);

            } else if (Utils.isTrue(mSelectedProgram.getIsMovie())) {
                mBackdrop.setImageResource(R.drawable.moviebanner);

            } else {
                mBackdrop.setImageResource(R.drawable.tvbanner);
            }
        }
    };

    public void setSelectedProgram(ProgramGridCell programView) {
        mSelectedProgramView = programView;
        mSelectedProgram = programView.getProgram();
        mHandler.removeCallbacks(detailUpdateTask);
        mHandler.postDelayed(detailUpdateTask, 500);
    }
}
