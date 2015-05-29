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
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.LiveTvChannelQuery;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.ProgramQuery;
import mediabrowser.model.livetv.SeriesTimerInfoDto;
import mediabrowser.model.results.ChannelInfoDtoResult;
import mediabrowser.model.results.ProgramInfoDtoResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.ui.GuideChannelHeader;
import tv.emby.embyatv.ui.GuidePagingButton;
import tv.emby.embyatv.ui.HorizontalScrollViewListener;
import tv.emby.embyatv.ui.ImageButton;
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

    private ProgramInfoDto mSelectedProgram;
    private ProgramGridCell mSelectedProgramView;

    private List<ChannelInfoDto> mAllChannels;
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
        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

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


    }

    private int getGuideHours() {
        return mFilters.any() ? FILTERED_HOURS : NORMAL_HOURS;
    }

    private void load() {
        fillTimeLine(getGuideHours());
        loadAllChannels();

    }

    @Override
    protected void onResume() {
        super.onResume();

        load();
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

        public void setContent(final ProgramInfoDto program) {
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
                    if (program.getIsSeries()) {
                        if (program.getSeriesTimerId() != null) {
                            // cancel series button
                            Button cancel = new Button(mActivity);
                            cancel.setText(getString(R.string.lbl_cancel_series));
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
    class RecordPopup {
        final int SERIES_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 540);
        final int NORMAL_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 400);
        final List<String> DayValues = Arrays.asList("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday");

        PopupWindow mPopup;
        String mProgramId;
        boolean mRecordSeries;

        LiveTvGuideActivity mActivity;
        TextView mDTitle;
        TextView mDSummary;
        LinearLayout mDTimeline;
        View mSeriesOptions;
        GridLayout mWeekdayOptions;
        CheckBox[] mWeekdayChecks = new CheckBox[7];
        EditText mPrePadding;
        EditText mPostPadding;
        CheckBox mPreRequired;
        CheckBox mPostRequired;
        CheckBox mOnlyNew;
        CheckBox mAnyTime;
        CheckBox mAnyChannel;
        Button mOkButton;
        Button mCancelButton;

        RecordPopup(LiveTvGuideActivity activity) {
            mActivity = activity;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.program_record_popup, null);
            mPopup = new PopupWindow(layout, mSummary.getWidth(), NORMAL_HEIGHT);
            mPopup.setFocusable(true);
            mPopup.setOutsideTouchable(true);
            mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
            mDTitle = (TextView)layout.findViewById(R.id.title);
            mDTitle.setTypeface(roboto);
            mDSummary = (TextView)layout.findViewById(R.id.summary);
            mDSummary.setTypeface(roboto);

            mPrePadding = (EditText) layout.findViewById(R.id.prePadding);
            mPostPadding = (EditText) layout.findViewById(R.id.postPadding);
            mPreRequired = (CheckBox) layout.findViewById(R.id.prePadReq);
            mPostRequired = (CheckBox) layout.findViewById(R.id.postPadReq);

            mOnlyNew = (CheckBox) layout.findViewById(R.id.onlyNew);
            mAnyChannel = (CheckBox) layout.findViewById(R.id.anyChannel);
            mAnyTime = (CheckBox) layout.findViewById(R.id.anyTime);

            mSeriesOptions = layout.findViewById(R.id.seriesOptions);
            mWeekdayOptions = (GridLayout) layout.findViewById(R.id.weekdayOptions);
            int i = -1;
            for (String day : DateFormatSymbols.getInstance().getWeekdays()) {
                if (i < 0) {
                    //first one is blank
                    i++;
                    continue;
                }
                CheckBox cbx = new CheckBox(mActivity);
                cbx.setText(day);
                cbx.setTextSize(14);
                mWeekdayChecks[i++] = cbx;
                mWeekdayOptions.addView(cbx);
            }

            mOkButton = (Button) layout.findViewById(R.id.okButton);
            mOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mProgramId, new Response<SeriesTimerInfoDto>() {
                        @Override
                        public void onResponse(SeriesTimerInfoDto response) {
                            response.setPrePaddingSeconds(Integer.valueOf(mPrePadding.getText().toString())*60);
                            response.setPostPaddingSeconds(Integer.valueOf(mPostPadding.getText().toString())*60);
                            response.setIsPrePaddingRequired(mPreRequired.isChecked());
                            response.setIsPostPaddingRequired(mPostRequired.isChecked());

                            if (mRecordSeries) {
                                response.setDays(new ArrayList<String>());
                                for (int i = 0; i < 7; i++) {
                                    if (mWeekdayChecks[i].isChecked()) response.getDays().add(DayValues.get(i));
                                }
                                response.setRecordNewOnly(mOnlyNew.isChecked());
                                response.setRecordAnyChannel(mAnyChannel.isChecked());
                                response.setRecordAnyTime(mAnyTime.isChecked());

                                TvApp.getApplication().getApiClient().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                                    @Override
                                    public void onResponse() {
                                        mPopup.dismiss();
                                        dismissProgramOptions();
                                        // we have to re-retrieve the program to get the timer id
                                        TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<ProgramInfoDto>() {
                                            @Override
                                            public void onResponse(ProgramInfoDto response) {
                                                mSelectedProgramView.setRecSeriesTimer(response.getSeriesTimerId());

                                            }
                                        });
                                        Utils.showToast(mActivity, R.string.msg_set_to_record);
                                    }

                                    @Override
                                    public void onError(Exception ex) {
                                        Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                    }

                                });

                            } else {
                                TvApp.getApplication().getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                                    @Override
                                    public void onResponse() {
                                        mPopup.dismiss();
                                        dismissProgramOptions();
                                        // we have to re-retrieve the program to get the timer id
                                        TvApp.getApplication().getApiClient().GetLiveTvProgramAsync(mProgramId, TvApp.getApplication().getCurrentUser().getId(), new Response<ProgramInfoDto>() {
                                            @Override
                                            public void onResponse(ProgramInfoDto response) {
                                                mSelectedProgramView.setRecTimer(response.getTimerId());
                                            }
                                        });
                                        Utils.showToast(mActivity, R.string.msg_set_to_record);
                                    }

                                    @Override
                                    public void onError(Exception ex) {
                                        Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                                    }
                                });
                            }
                        }
                    });
                }
            });
            mCancelButton = (Button) layout.findViewById(R.id.cancelButton);
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopup.dismiss();
                }
            });

            mDTimeline = (LinearLayout) layout.findViewById(R.id.timeline);
        }

        public boolean isShowing() {
            return (mPopup != null && mPopup.isShowing());
        }

        public void setContent(ProgramInfoDto program, SeriesTimerInfoDto defaults, boolean recordSeries) {
            mProgramId = program.getId();
            mRecordSeries = recordSeries;

            mDTitle.setText(program.getName());
            mDSummary.setText(program.getOverview());
            if (mDSummary.getLineCount() < 2) {
                mDSummary.setGravity(Gravity.CENTER);
            } else {
                mDSummary.setGravity(Gravity.LEFT);
            }

            //if already started then can't require pre padding
            Date local = Utils.convertToLocalDate(program.getStartDate());
            Date now = new Date();
            mPreRequired.setEnabled(local.getTime() > now.getTime());

            // build timeline info
            setTimelineRow(mDTimeline, program);

            // set defaults
            mPrePadding.setText(String.valueOf(defaults.getPrePaddingSeconds()/60));
            mPostPadding.setText(String.valueOf(defaults.getPostPaddingSeconds()/60));
            mPreRequired.setChecked(defaults.getIsPrePaddingRequired());
            mPostRequired.setChecked(defaults.getIsPostPaddingRequired());

            if (recordSeries) {
                mPopup.setHeight(SERIES_HEIGHT);
                mSeriesOptions.setVisibility(View.VISIBLE);

                // select proper days
                int i = 0;
                for (CheckBox day : mWeekdayChecks) {
                    day.setChecked(defaults.getDays().contains(DayValues.get(i)));
                    i++;
                }

                // and other options
                mAnyChannel.setChecked(defaults.getRecordAnyChannel());
                mOnlyNew.setChecked(defaults.getRecordNewOnly());
                mAnyTime.setChecked(defaults.getRecordAnyTime());

            } else {
                mPopup.setHeight(NORMAL_HEIGHT);
                mSeriesOptions.setVisibility(View.GONE);
            }

        }

        public void show() {
            mPopup.showAtLocation(mImage, Gravity.NO_GRAVITY, mTitle.getLeft(), mTitle.getTop() - 10);
            mOkButton.requestFocus();

        }

        public void dismiss() {
            if (mPopup != null && mPopup.isShowing()) mPopup.dismiss();
        }
    }

    private void setTimelineRow(LinearLayout timelineRow, ProgramInfoDto program) {
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
        if (mRecordPopup == null) mRecordPopup = new RecordPopup(this);
        TvApp.getApplication().getApiClient().GetDefaultLiveTvTimerInfo(mSelectedProgram.getId(), new Response<SeriesTimerInfoDto>() {
            @Override
            public void onResponse(SeriesTimerInfoDto response) {
                mRecordPopup.setContent(mSelectedProgram, response, recordSeries);
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
        layout.addView(btn);
        return btn;
    }

    private void loadAllChannels() {
        //Get channels
        LiveTvChannelQuery query = new LiveTvChannelQuery();
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setEnableFavoriteSorting(true);
        TvApp.getApplication().getApiClient().GetLiveTvChannelsAsync(query, new Response<ChannelInfoDtoResult>() {
            @Override
            public void onResponse(ChannelInfoDtoResult response) {
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
        int displayedChannels = 0;

        @Override
        protected void onPreExecute() {
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

                    // put focus on the last tuned channel
                    if (channel.getId().equals(mFirstFocusChannelId)) {
                        firstRow = row;
                        mFirstFocusChannelId = null; // only do this first time in not while paging around
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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

    private List<ProgramInfoDto> getProgramsForChannel(String channelId, ProgramInfoDto[] programs) {
        List<ProgramInfoDto> results = new ArrayList<>();
        boolean passes = !mFilters.any();
        for (ProgramInfoDto program : programs) {
            if (program.getChannelId().equals(channelId) && Utils.convertToLocalDate(program.getEndDate()).getTime() > mCurrentLocalGuideStart) {
                results.add(program);
                passes |= mFilters.passesFilter(program);
            }
        }

        return passes ? results : new ArrayList<ProgramInfoDto>();
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
            //url = "https://image.tmdb.org/t/p/w396/zr2p353wrd6j3wjLgDT4TcaestB.jpg";
            Picasso.with(mActivity).load(url).resize(IMAGE_SIZE, IMAGE_SIZE).centerInside().into(mImage);

            mInfoRow.removeAllViews();
            // fake
//            mSelectedProgram.setCommunityRating(7.5f);
//            InfoLayoutHelper.addCriticInfo(mActivity, mSelectedProgram, mInfoRow);
//            InfoLayoutHelper.addSpacer(mActivity, mInfoRow, " 2003  ", 14);
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

    public void setSelectedProgram(ProgramGridCell programView) {
        mSelectedProgramView = programView;
        mSelectedProgram = programView.getProgram();
        mHandler.removeCallbacks(detailUpdateTask);
        mHandler.postDelayed(detailUpdateTask, 500);
    }
}
