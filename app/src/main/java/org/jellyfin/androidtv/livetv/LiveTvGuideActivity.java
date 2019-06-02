package org.jellyfin.androidtv.livetv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.base.IMessageListener;
import org.jellyfin.androidtv.model.LiveTvPrefs;
import org.jellyfin.androidtv.ui.FriendlyDateButton;
import org.jellyfin.androidtv.ui.GuideChannelHeader;
import org.jellyfin.androidtv.ui.GuidePagingButton;
import org.jellyfin.androidtv.ui.HorizontalScrollViewListener;
import org.jellyfin.androidtv.ui.LiveProgramDetailPopup;
import org.jellyfin.androidtv.ui.ObservableHorizontalScrollView;
import org.jellyfin.androidtv.ui.ObservableScrollView;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.ScrollViewListener;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

public class LiveTvGuideActivity extends BaseActivity implements ILiveTvGuide {
    public static final int ROW_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(),55);
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),7);
    private static final int IMAGE_SIZE = Utils.convertDpToPixel(TvApp.getApplication(), 150);
    public static final int PAGEBUTTON_HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 20);
    public static final int PAGEBUTTON_WIDTH = 120 * PIXELS_PER_MINUTE;
    public static final int PAGE_SIZE = 75;
    public static final int NORMAL_HOURS = 9;
    public static final int FILTERED_HOURS = 4;

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
    private View mResetButton;

    private BaseItemDto mSelectedProgram;
    private ProgramGridCell mSelectedProgramView;
    private long mLastLoad = 0;

    private List<ChannelInfoDto> mAllChannels;
    private String mFirstFocusChannelId;
    private boolean focusAtEnd;
    private GuideFilters mFilters = new GuideFilters();

    private Calendar mCurrentGuideStart;
    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart = System.currentTimeMillis();
    private long mCurrentLocalGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;
    private long mLastFocusChanged;
    private boolean mLoadLastChannel;

    private Handler mHandler = new Handler();

    private Typeface roboto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        roboto = TvApp.getApplication().getDefaultFont();

        setContentView(R.layout.live_tv_guide);

        mDisplayDate = findViewById(R.id.displayDate);
        mTitle = findViewById(R.id.title);
        mTitle.setTypeface(roboto);
        mSummary = findViewById(R.id.summary);
        mSummary.setTypeface(roboto);
        mChannelStatus = findViewById(R.id.channelsStatus);
        mFilterStatus = findViewById(R.id.filterStatus);
        mChannelStatus.setTypeface(roboto);
        mFilterStatus.setTypeface(roboto);
        mChannelStatus.setTextColor(Color.GRAY);
        mFilterStatus.setTextColor(Color.GRAY);
        mInfoRow = findViewById(R.id.infoRow);
        mImage = findViewById(R.id.programImage);
        mBackdrop = findViewById(R.id.backdrop);
        mChannels = findViewById(R.id.channels);
        mTimeline = findViewById(R.id.timeline);
        mProgramRows = findViewById(R.id.programRows);
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setVisibility(View.VISIBLE);

        findViewById(R.id.filterButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterOptions();
            }
        });

        findViewById(R.id.optionsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptions();
            }
        });

        findViewById(R.id.dateButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        mResetButton = findViewById(R.id.resetButton);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageGuideTo(System.currentTimeMillis());
            }
        });

        mProgramRows.setFocusable(false);
        mChannelScroller = findViewById(R.id.channelScroller);
        ObservableScrollView programVScroller = findViewById(R.id.programVScroller);
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x, y);
            }
        });

        mTimelineScroller = findViewById(R.id.timelineHScroller);
        mTimelineScroller.setFocusable(false);
        mTimelineScroller.setFocusableInTouchMode(false);
        mTimeline.setFocusable(false);
        mTimeline.setFocusableInTouchMode(false);
        mChannelScroller.setFocusable(false);
        mChannelScroller.setFocusableInTouchMode(false);
        ObservableHorizontalScrollView programHScroller = findViewById(R.id.programHScroller);
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

        //auto launch channel if indicated
        mLoadLastChannel = getIntent().getBooleanExtra("loadLast", false);
    }

    private int getGuideHours() {
        return mFilters.any() ? FILTERED_HOURS : NORMAL_HOURS;
    }

    private void load() {
        fillTimeLine(mCurrentLocalGuideStart, getGuideHours());
        TvManager.loadAllChannels(new Response<Integer>() {
            @Override
            public void onResponse(Integer ndx) {
                if (ndx  >= PAGE_SIZE) {
                    // last channel is not in first page so grab a set where it will be in the middle
                    ndx = ndx - (PAGE_SIZE / 2);
                } else {
                    ndx = 0; // just start at beginning
                }

                mLastLoad = System.currentTimeMillis();

                mAllChannels = TvManager.getAllChannels();
                if (mAllChannels.size() > 0) {
                    displayChannels(ndx, PAGE_SIZE);
                } else {
                    mSpinner.setVisibility(View.GONE);
                }
            }
        });
    }

    private void reload() {
        fillTimeLine(mCurrentLocalGuideStart, getGuideHours());
        displayChannels(mCurrentDisplayChannelStartNdx, PAGE_SIZE);
        mLastLoad = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mLoadLastChannel) {
            mLoadLastChannel = false;
            String channel = TvManager.getLastLiveTvChannel();
            if (channel != null) {
                PlaybackHelper.retrieveAndPlay(channel, false, this);
            } else {
                doLoad();
            }
        } else {
            doLoad();
        }
    }

    protected void doLoad() {
        if (TvManager.shouldForceReload() || System.currentTimeMillis() > mLastLoad + 3600000) {
            if (mAllChannels == null) {
                mAllChannels = TvManager.getAllChannels();
                if (mAllChannels == null) {
                    load();
                } else {
                    reload();
                }
            } else {
                reload();
            }

            mFirstFocusChannelId = TvManager.getLastLiveTvChannel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mDisplayProgramsTask != null) {
            mDisplayProgramsTask.cancel(true);
        }
        if (mDetailPopup != null) {
            mDetailPopup.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCurrentLocalGuideStart > System.currentTimeMillis()) {
            TvManager.forceReload(); //we paged ahead - force a re-load if we come back in
        }
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
                if ((mDetailPopup == null || !mDetailPopup.isShowing())
                        && (mFilterPopup == null || !mFilterPopup.isShowing())
                        && (mOptionsPopup == null || !mOptionsPopup.isShowing())
                        && mSelectedProgram != null
                        && mSelectedProgram.getChannelId() != null) {
                    // tune to the current channel
                    Utils.beep();
                    PlaybackHelper.retrieveAndPlay(mSelectedProgram.getChannelId(), false, this);
                    return true;
                }

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && mSelectedProgramView.isLast()
                        && System.currentTimeMillis() - mLastFocusChanged > 1000) {
                    requestGuidePage(mCurrentLocalGuideEnd);
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (getCurrentFocus() instanceof ProgramGridCell
                        && mSelectedProgramView != null
                        && mSelectedProgramView.isFirst()
                        && TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate()).getTime() > System.currentTimeMillis()
                        && System.currentTimeMillis() - mLastFocusChanged > 1000) {
                    focusAtEnd = true;
                    requestGuidePage(mCurrentLocalGuideStart - (getGuideHours()*60*60000));
                }
                break;
        }

        return super.onKeyUp(keyCode, event);
    }

    View.OnClickListener datePickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pageGuideTo(((FriendlyDateButton)v).getDate());
            dateDialog.dismiss();
        }
    };

    AlertDialog dateDialog;

    private void showDatePicker() {
        FrameLayout scrollPane = (FrameLayout) getLayoutInflater().inflate(R.layout.horizontal_scroll_pane, null);
        LinearLayout scrollItems = scrollPane.findViewById(R.id.scrollItems);
        for (long increment = 0; increment < 15; increment++) {
            scrollItems.addView(new FriendlyDateButton(this, System.currentTimeMillis() + (increment * 86400000), datePickedListener));
        }

        dateDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.lbl_select_date)
                .setView(scrollPane)
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();

    }

    private void requestGuidePage(final long startTime) {
        new AlertDialog.Builder(mActivity)
                .setTitle("Load Guide Data")
                .setMessage("Load " + (startTime > mCurrentLocalGuideStart ? "next " : "previous ") +getGuideHours()+" hours?")
                .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pageGuideTo(startTime);
                    }
                })
                .setNegativeButton(R.string.lbl_no, null)
                .show();
    }

    private void pageGuideTo(long startTime) {
        if (startTime < System.currentTimeMillis()) startTime = System.currentTimeMillis(); // don't allow the past
        TvApp.getApplication().getLogger().Info("page to "+new Date(startTime));
        TvManager.forceReload(); // don't allow cache
        if (mSelectedProgram != null) {
            mFirstFocusChannelId = mSelectedProgram.getChannelId();
        }
        fillTimeLine(startTime, getGuideHours());
        loadProgramData();
    }

    private LiveProgramDetailPopup mDetailPopup;

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
            mMovies = layout.findViewById(R.id.movies);
            mSeries = layout.findViewById(R.id.series);
            mNews = layout.findViewById(R.id.news);
            mKids = layout.findViewById(R.id.kids);
            mSports = layout.findViewById(R.id.sports);
            mPremiere = layout.findViewById(R.id.premiere);

            mFilterButton = layout.findViewById(R.id.okButton);
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
            mClearButton = layout.findViewById(R.id.clearButton);
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

    private OptionsPopup mOptionsPopup;
    class OptionsPopup {

        final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 300);
        final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 460);

        PopupWindow mPopup;
        LiveTvGuideActivity mActivity;
        CheckBox mHd;
        CheckBox mLive;
        CheckBox mRepeat;
        CheckBox mNew;
        CheckBox mColorCode;
        CheckBox mFavTop;
        Spinner mSortBy;
        CheckBox mPremiere;
        String mCurrentSort;

        Button mSaveButton;
        Button mCancelButton;

        OptionsPopup(LiveTvGuideActivity activity) {
            mActivity = activity;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.guide_options_popup, null);
            mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
            mPopup.setFocusable(true);
            mPopup.setOutsideTouchable(true);
            mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
            mPopup.setAnimationStyle(R.style.PopupSlideInRight);
            mHd = layout.findViewById(R.id.hd);
            mRepeat = layout.findViewById(R.id.repeat);
            mLive = layout.findViewById(R.id.live);
            mNew = layout.findViewById(R.id.newEpisodes);
            mColorCode = layout.findViewById(R.id.colorCode);
            mPremiere = layout.findViewById(R.id.premieres);
            mFavTop = layout.findViewById(R.id.favTop);
            mSortBy = layout.findViewById(R.id.sortBy);

            mSaveButton = layout.findViewById(R.id.okButton);
            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LiveTvPrefs prefs = TvManager.getPrefs();
                    prefs.showHDIndicator = mHd.isChecked();
                    prefs.showPremiereIndicator = mPremiere.isChecked();
                    prefs.showNewIndicator = mNew.isChecked();
                    prefs.favsAtTop = mFavTop.isChecked();
                    prefs.colorCodeGuide = mColorCode.isChecked();
                    prefs.showRepeatIndicator = mRepeat.isChecked();
                    prefs.channelOrder = mCurrentSort;

                    TvManager.updatePrefs(prefs);

                    load();
                    mPopup.dismiss();
                }
            });
            mCancelButton = layout.findViewById(R.id.cancelButton);
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopup.dismiss();
                }
            });

            mSortBy.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, new ArrayList<>(Arrays.asList(getString(R.string.lbl_guide_option_played), getString(R.string.lbl_guide_option_number)))));

            mSortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mCurrentSort = position == 0 ? "DatePlayed" : "Number";
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }

        public boolean isShowing() {
            return (mPopup != null && mPopup.isShowing());
        }

        public void show() {
            LiveTvPrefs prefs = TvManager.getPrefs();

            mHd.setChecked(prefs.showHDIndicator);
            mRepeat.setChecked(prefs.showRepeatIndicator);
            mLive.setChecked(prefs.showLiveIndicator);
            mNew.setChecked(prefs.showNewIndicator);
            mRepeat.setChecked(prefs.showRepeatIndicator);
            mColorCode.setChecked(prefs.colorCodeGuide);
            mPremiere.setChecked(prefs.showPremiereIndicator);
            mSortBy.setSelection(prefs.channelOrder.equals("DatePlayed") ? 0 : 1);

            mPopup.showAtLocation(mTimelineScroller, Gravity.NO_GRAVITY, mTimelineScroller.getRight(), mSummary.getTop()-20);
        }

        public void dismiss() {
            if (mPopup != null && mPopup.isShowing()) {
                mPopup.dismiss();
            }
        }
    }

    public void dismissProgramOptions() {
        if (mDetailPopup != null) {
            mDetailPopup.dismiss();
        }
    }

    public void showProgramOptions() {
        if (mSelectedProgram == null) return;
        if (mDetailPopup == null) {
            mDetailPopup = new LiveProgramDetailPopup(this, mSummary.getWidth()+20, new EmptyResponse() {
                @Override
                public void onResponse() {
                    PlaybackHelper.retrieveAndPlay(mSelectedProgram.getChannelId(), false, mActivity);
                }
            });
        }

        mDetailPopup.setContent(mSelectedProgram, mSelectedProgramView);
        mDetailPopup.show(mImage, mTitle.getLeft(), mTitle.getTop() - 10);
    }

    public void showFilterOptions() {
        if (mFilterPopup == null) {
            mFilterPopup = new FilterPopup(this);
        }
        mFilterPopup.show();
    }


    public void showOptions() {
        if (mOptionsPopup == null) {
            mOptionsPopup = new OptionsPopup(this);
        }
        mOptionsPopup.show();
    }

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) {
            end = mAllChannels.size();
        }

        if (mFilters.any()) {
            // if we are filtered, then we need to get programs for all channels
            mCurrentDisplayChannelStartNdx = 0;
            mCurrentDisplayChannelEndNdx = mAllChannels.size()-1;
        } else {
            mCurrentDisplayChannelStartNdx = start;
            mCurrentDisplayChannelEndNdx = end - 1;
        }
        TvApp.getApplication().getLogger().Debug("*** Display channels pre-execute");
        mSpinner.setVisibility(View.VISIBLE);

        loadProgramData();
    }

    private void loadProgramData() {
        mProgramRows.removeAllViews();
        mChannels.removeAllViews();
        mChannelStatus.setText("");
        mFilterStatus.setText("");
        TvManager.getProgramsAsync(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyResponse() {
            @Override
            public void onResponse() {
                TvApp.getApplication().getLogger().Debug("*** Programs response");
                if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
                mDisplayProgramsTask = new DisplayProgramsTask();
                mDisplayProgramsTask.execute(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx);
            }
        });
    }

    DisplayProgramsTask mDisplayProgramsTask;
    class DisplayProgramsTask extends AsyncTask<Integer, Integer, Void> {

        View firstFocusView;
        int displayedChannels = 0;

        @Override
        protected void onPreExecute() {
            TvApp.getApplication().getLogger().Debug("*** Display programs pre-execute");
            mChannels.removeAllViews();
            mProgramRows.removeAllViews();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) {
                    pageUpStart = 0;
                }

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(LiveTvGuideActivity.PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);
                displayedChannels = 0;

                mProgramRows.addView(new GuidePagingButton(mActivity, mActivity, pageUpStart, getString(R.string.lbl_load_channels)+mAllChannels.get(pageUpStart).getNumber() + " - "+mAllChannels.get(mCurrentDisplayChannelStartNdx-1).getNumber()));
            }
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int start = params[0];
            int end = params[1];
            boolean first = true;

            TvApp.getApplication().getLogger().Debug("*** About to iterate programs");
            LinearLayout prevRow = null;
            for (int i = start; i <= end; i++) {
                if (isCancelled()) return null;
                final ChannelInfoDto channel = TvManager.getChannel(i);
                List<BaseItemDto> programs = TvManager.getProgramsForChannel(channel.getId(), mFilters);
                final LinearLayout row = getProgramRow(programs, channel.getId());
                if (row == null) continue; // no row to show

                if (first) {
                    first = false;
                    firstFocusView = row;
                }

                // set focus parameters if we are not on first row
                // this makes focus movements more predictable for the grid view
                if (prevRow != null) {
                    TvManager.setFocusParms(row, prevRow, true);
                    TvManager.setFocusParms(prevRow, row, false);
                }
                prevRow = row;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GuideChannelHeader header = new GuideChannelHeader(mActivity, channel);
                        mChannels.addView(header);
                        header.loadImage();
                        mProgramRows.addView(row);
                        // put focus on the last tuned channel
                        if (channel.getId().equals(mFirstFocusChannelId)) {
                            firstFocusView = focusAtEnd ? row.getChildAt(row.getChildCount()-1) : row;
                            focusAtEnd = false;
                            mFirstFocusChannelId = null;
                        }

                    }
                });

                displayedChannels++;
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

                mProgramRows.addView(new GuidePagingButton(mActivity, mActivity, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels)+mAllChannels.get(mCurrentDisplayChannelEndNdx+1).getNumber() + " - "+mAllChannels.get(pageDnEnd).getNumber()));
            }

            mChannelStatus.setText(displayedChannels+" of "+mAllChannels.size()+" channels");
            mFilterStatus.setText(mFilters.toString() + " for "+getGuideHours()+" hours");
            mFilterStatus.setTextColor(mFilters.any() ? Color.WHITE : Color.GRAY);

            mResetButton.setVisibility(mCurrentLocalGuideStart > System.currentTimeMillis() ? View.VISIBLE : View.GONE); // show reset button if paged ahead

            mSpinner.setVisibility(View.GONE);
            if (firstFocusView != null) {
                firstFocusView.requestFocus();
            }
        }
    }

    private int currentCellId = 0;

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {

        LinearLayout programRow = new LinearLayout(this);

        if (programs.size() == 0) {
            if (mFilters.any()) return null; // don't show rows with no program data

            BaseItemDto empty = new BaseItemDto();
            int duration = ((Long)((mCurrentLocalGuideEnd - mCurrentLocalGuideStart) / 60000)).intValue();
            empty.setName("  <No Program Data Available>");
            empty.setChannelId(channelId);
            empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart)));
            empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart+(duration*60000))));
            ProgramGridCell cell = new ProgramGridCell(this, this, empty);
            cell.setId(currentCellId++);
            cell.setLayoutParams(new ViewGroup.LayoutParams(duration * PIXELS_PER_MINUTE, ROW_HEIGHT));
            cell.setFocusable(true);
            programRow.addView(cell);
            cell.setLast();
            cell.setFirst();
            return programRow;
        }

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? TimeUtils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) {
                start = getCurrentLocalStartDate();
            }
            if (start > prevEnd) {
                // fill empty time slot
                BaseItemDto empty = new BaseItemDto();
                empty.setName("  <No Program Data Available>");
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
                Long duration = (start - prevEnd);
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd+duration)));
                ProgramGridCell cell = new ProgramGridCell(this, this, empty);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long)(duration / 60000)).intValue() * PIXELS_PER_MINUTE, ROW_HEIGHT));
                cell.setFocusable(true);
                if (prevEnd == mCurrentLocalGuideStart) {
                    cell.setFirst();
                }
                programRow.addView(cell);
            }
            long end = item.getEndDate() != null ? TimeUtils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end - start) / 60000;
            //TvApp.getApplication().getLogger().Debug("Duration for "+item.getName()+" is "+duration.intValue());
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(this, this, item);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * PIXELS_PER_MINUTE, ROW_HEIGHT));
                program.setFocusable(true);
                if (start == mCurrentLocalGuideStart) {
                    program.setFirst();
                }
                if (end == mCurrentLocalGuideEnd) {
                    program.setLast();
                }

                programRow.addView(program);
            }
        }

        //If not at end of time period - fill in the rest
        if (prevEnd < mCurrentLocalGuideEnd) {
            // fill empty time slot
            BaseItemDto empty = new BaseItemDto();
            empty.setName("  <No Program Data Available>");
            empty.setChannelId(channelId);
            empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
            Long duration = (mCurrentLocalGuideEnd - prevEnd);
            empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd+duration)));
            ProgramGridCell cell = new ProgramGridCell(this, this, empty);
            cell.setId(currentCellId++);
            cell.setLayoutParams(new ViewGroup.LayoutParams(((Long)(duration / 60000)).intValue() * PIXELS_PER_MINUTE, ROW_HEIGHT));
            cell.setFocusable(true);
            programRow.addView(cell);
        }

        return programRow;
    }

    private void fillTimeLine(long start, int hours) {
        mCurrentGuideStart = Calendar.getInstance();
        mCurrentGuideStart.setTime(new Date(start));
        mCurrentGuideStart.set(Calendar.MINUTE, mCurrentGuideStart.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        mCurrentGuideStart.set(Calendar.SECOND, 0);
        mCurrentGuideStart.set(Calendar.MILLISECOND, 0);
        mCurrentLocalGuideStart = mCurrentGuideStart.getTimeInMillis();

        mDisplayDate.setText(TimeUtils.getFriendlyDate(mCurrentGuideStart.getTime()));
        Calendar current = (Calendar) mCurrentGuideStart.clone();
        mCurrentGuideEnd = (Calendar) mCurrentGuideStart.clone();
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

    public long getCurrentLocalStartDate() { return mCurrentLocalGuideStart; }
    public long getCurrentLocalEndDate() { return mCurrentLocalGuideEnd; }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (mSelectedProgram.getOverview() == null && mSelectedProgram.getId() != null) {
                TvApp.getApplication().getApiClient().GetItemAsync(mSelectedProgram.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        mSelectedProgram = response;
                        detailUpdateInternal();
                    }

                    @Override
                    public void onError(Exception exception) {
                        TvApp.getApplication().getLogger().ErrorException("Unable to get program details", exception);
                        detailUpdateInternal();
                    }
                });
            } else {
                detailUpdateInternal();
            }
        }
    };

    private void detailUpdateInternal() {
        if (mSelectedProgram == null) return;

        mTitle.setText(mSelectedProgram.getName());
        mSummary.setText(mSelectedProgram.getOverview());
        if (mSelectedProgram.getId() != null) {
            mDisplayDate.setText(TimeUtils.getFriendlyDate(TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));
            String url = ImageUtils.getPrimaryImageUrl(mSelectedProgram, TvApp.getApplication().getApiClient());
            Picasso.with(mActivity).load(url).resize(IMAGE_SIZE, IMAGE_SIZE).centerInside().into(mImage);

            //info row
            InfoLayoutHelper.addInfoRow(mActivity, mSelectedProgram, mInfoRow, false, false);

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
        } else {
            mInfoRow.removeAllViews();
            mBackdrop.setImageResource(R.drawable.tvbanner);
            mImage.setImageResource(R.drawable.blank10x10);
        }
    }

    public void setSelectedProgram(ProgramGridCell programView) {
        mSelectedProgramView = programView;
        mSelectedProgram = programView.getProgram();
        mHandler.removeCallbacks(detailUpdateTask);
        mHandler.postDelayed(detailUpdateTask, 500);
        mLastFocusChanged = System.currentTimeMillis();
    }
}
