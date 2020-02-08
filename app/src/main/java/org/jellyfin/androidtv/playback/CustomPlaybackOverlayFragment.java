package org.jellyfin.androidtv.playback;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.CustomMessage;
import org.jellyfin.androidtv.base.IMessageListener;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.livetv.TvManager;
import org.jellyfin.androidtv.model.compat.StreamInfo;
import org.jellyfin.androidtv.model.compat.SubtitleStreamInfo;
import org.jellyfin.androidtv.presentation.CardPresenter;
import org.jellyfin.androidtv.presentation.ChannelCardPresenter;
import org.jellyfin.androidtv.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.AudioDelayPopup;
import org.jellyfin.androidtv.ui.GuideChannelHeader;
import org.jellyfin.androidtv.ui.GuidePagingButton;
import org.jellyfin.androidtv.ui.HorizontalScrollViewListener;
import org.jellyfin.androidtv.ui.ImageButton;
import org.jellyfin.androidtv.ui.LiveProgramDetailPopup;
import org.jellyfin.androidtv.ui.ObservableHorizontalScrollView;
import org.jellyfin.androidtv.ui.ObservableScrollView;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.ScrollViewListener;
import org.jellyfin.androidtv.ui.ValueChangedListener;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.RemoteControlReceiver;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.ChapterInfoDto;
import org.jellyfin.apiclient.model.dto.ImageOptions;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackEvent;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

public class CustomPlaybackOverlayFragment extends Fragment implements IPlaybackOverlayFragment, ILiveTvGuide {
    ImageView mPoster;
    ImageView mStudioImage;
    ImageView mLogoImage;
    TextView mTitle;
    TextView mEndTime;
    TextView mCurrentPos;
    TextView mRemainingTime;
    TextView mInfoSummary;
    View mTopPanel;
    View mBottomPanel;
    ImageButton mPlayPauseBtn;
    LinearLayout mInfoRow;
    LinearLayout mButtonRow;
    FrameLayout mPopupArea;
    RowsSupportFragment mPopupRowsFragment;
    ArrayObjectAdapter mPopupRowAdapter;
    ListRow mChapterRow;
    PositionableListRowPresenter mPopupRowPresenter;
    ProgressBar mCurrentProgress;
    CustomPlaybackOverlayFragment mFragment;

    View mNextUpPanel;
    View mSmNextUpPanel;
    Button mSmNextButton;
    Button mSmCancelButton;
    TextView mSmNextUpTitle;
    TextView mSmStartsIn;
    Button mNextButton;
    Button mCancelButton;
    TextView mNextUpTitle;
    TextView mNextUpSummary;
    TextView mStartsIn;
    LinearLayout mNextUpInfoRow;
    ImageView mNextUpPoster;
    TextView mSubtitleText;

    //Live guide items
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(),7);
    public static final int PAGE_SIZE = 75;
    RelativeLayout mTvGuide;
    private TextView mDisplayDate;
    private TextView mGuideTitle;
    private TextView mGuideCurrentTitle;
    private TextView mChannelStatus;
    private TextView mFilterStatus;
    private TextView mSummary;
    private LinearLayout mGuideInfoRow;
    private LinearLayout mChannels;
    private LinearLayout mTimeline;
    private LinearLayout mProgramRows;
    private ScrollView mChannelScroller;
    private HorizontalScrollView mTimelineScroller;
    private View mGuideSpinner;

    private BaseItemDto mSelectedProgram;
    private ProgramGridCell mSelectedProgramView;
    private boolean mGuideVisible = false;
    private Calendar mCurrentGuideStart;
    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart;
    private long mCurrentLocalGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;
    private int mGuideHours = 6;
    private List<ChannelInfoDto> mAllChannels;
    private String mFirstFocusChannelId;

    PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay;

    Animation fadeOut;
    Animation slideUp;
    Animation slideDown;
    Animation showPopup;
    Animation hidePopup;
    Animation showNextUp;
    Animation hideNextUp;
    Animation showSmNextUp;
    Animation hideSmNextUp;
    Handler mHandler = new Handler();
    Runnable mHideTask;

    TvApp mApplication;
    PlaybackOverlayActivity mActivity;
    private AudioManager mAudioManager;

    int mButtonSize;

    boolean mFadeEnabled = false;
    boolean mIsVisible = false;
    boolean mPopupPanelVisible = false;
    boolean mNextUpPanelVisible = false;
    boolean mSmNextUpPanelVisible = false;

    int mCurrentDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = this;
        mApplication = TvApp.getApplication();
        mActivity = (PlaybackOverlayActivity) getActivity();

        // stop any audio that may be playing
        MediaManager.stopAudio();

        mAudioManager = (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            mApplication.getLogger().Error("Unable to get audio manager");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        mActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mActivity.setKeyListener(keyListener);

        mItemsToPlay = MediaManager.getCurrentVideoQueue();
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(mApplication, mApplication.getString(R.string.msg_no_playable_items));
            mActivity.finish();
            return;
        }

        mButtonSize = Utils.convertDpToPixel(mActivity, 28);

        mApplication.setPlaybackController(new PlaybackController(mItemsToPlay, this));
        mPlaybackController = mApplication.getPlaybackController();

        // setup fade task
        mHideTask = new Runnable() {
            @Override
            public void run() {
                if (mIsVisible) hide();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.vlc_player_interface, container);

        // inject the RowsSupportFragment in the popup container
        if (getChildFragmentManager().findFragmentById(R.id.rows_area) == null) {
            mPopupRowsFragment = new RowsSupportFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.rows_area, mPopupRowsFragment).commit();
        } else {
            mPopupRowsFragment = (RowsSupportFragment) getChildFragmentManager()
                    .findFragmentById(R.id.rows_area);
        }

        mPopupRowPresenter = new PositionableListRowPresenter();
        mPopupRowAdapter = new ArrayObjectAdapter(mPopupRowPresenter);
        mPopupRowsFragment.setAdapter(mPopupRowAdapter);
        mPopupRowsFragment.setOnItemViewClickedListener(itemViewClickedListener);

        // And the Live Guide element
        mTvGuide = (RelativeLayout) inflater.inflate(R.layout.overlay_tv_guide, null);
        root.addView(mTvGuide);
        mTvGuide.setVisibility(View.GONE);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //if we're not visible, show us
                if (!mIsVisible) show();

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();

                TvApp.getApplication().getLogger().Debug("Got touch event.");
                return false;
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) return;

        mPoster = mActivity.findViewById(R.id.poster);
        mNextUpPoster = mActivity.findViewById(R.id.nextUpPoster);
        mStudioImage = mActivity.findViewById(R.id.studioImg);
        mLogoImage = mActivity.findViewById(R.id.logoImage);
        mTopPanel = mActivity.findViewById(R.id.topPanel);
        mBottomPanel = mActivity.findViewById(R.id.bottomPanel);
        mNextUpPanel = mActivity.findViewById(R.id.nextUpPanel);
        mSmNextUpPanel = mActivity.findViewById(R.id.smNextUpPanel);

        mPlayPauseBtn = mActivity.findViewById(R.id.playPauseBtn);
        mPlayPauseBtn.setSecondaryImage(R.drawable.ic_pause);
        mPlayPauseBtn.setPrimaryImage(R.drawable.ic_play);
        mPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackController.playPause();
            }
        });
        mInfoRow = mActivity.findViewById(R.id.infoRow);
        mNextUpInfoRow = mActivity.findViewById(R.id.nextUpInfoRow);
        mButtonRow = mActivity.findViewById(R.id.buttonRow);
        mInfoSummary = mActivity.findViewById(R.id.infoSummary);
        mTitle = mActivity.findViewById(R.id.title);
        mNextUpTitle = mActivity.findViewById(R.id.nextUpTitle);
        mSmNextUpTitle = mActivity.findViewById(R.id.sm_upnext_title);
        mNextUpSummary = mActivity.findViewById(R.id.nextUpSummary);
        Typeface font = Typeface.createFromAsset(mActivity.getAssets(), "fonts/Roboto-Light.ttf");
        mTitle.setTypeface(font);
        mNextUpTitle.setTypeface(font);
        mSmNextUpTitle.setTypeface(font);
        mNextUpSummary.setTypeface(font);
        mInfoSummary.setTypeface(font);
        mEndTime = mActivity.findViewById(R.id.endTime);
        mCurrentPos = mActivity.findViewById(R.id.currentPos);
        mRemainingTime = mActivity.findViewById(R.id.remainingTime);
        mCurrentProgress = mActivity.findViewById(R.id.playerProgress);
        mPopupArea = mActivity.findViewById(R.id.popupArea);
        mStartsIn = mActivity.findViewById(R.id.startsIn);
        mNextButton = mActivity.findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackController.next();
            }
        });
        mCancelButton = mActivity.findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mSmStartsIn = mActivity.findViewById(R.id.sm_upnext_startsin);
        mSmNextButton = mActivity.findViewById(R.id.btn_sm_upnext_next);
        mSmNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaybackController.next();
            }
        });
        mSmCancelButton = mActivity.findViewById(R.id.btn_sm_upnext_cancel);
        mSmCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //manual subtitles
        mSubtitleText = mActivity.findViewById(R.id.offLine_subtitleText);
        mSubtitleText.setTextSize(32);
        mSubtitleText.setShadowLayer(1.6f,1.5f,1.3f, Color.BLACK);
        updateManualSubtitlePosition();

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        slideUp.setAnimationListener(showAnimationListener);
        setupPopupAnimations();
        setupNextUpAnimations();

        //live guide
        Typeface roboto = TvApp.getApplication().getDefaultFont();
        mDisplayDate = mActivity.findViewById(R.id.displayDate);
        mGuideTitle = mActivity.findViewById(R.id.guideTitle);
        mGuideTitle.setTypeface(roboto);
        mGuideCurrentTitle = mActivity.findViewById(R.id.guideCurrentTitle);
        mGuideCurrentTitle.setTypeface(roboto);
        mSummary = mActivity.findViewById(R.id.summary);
        mSummary.setTypeface(roboto);
        mChannelStatus = mActivity.findViewById(R.id.channelsStatus);
        mFilterStatus = mActivity.findViewById(R.id.filterStatus);
        mChannelStatus.setTypeface(roboto);
        mFilterStatus.setTypeface(roboto);
        mChannelStatus.setTextColor(Color.GRAY);
        mFilterStatus.setTextColor(Color.GRAY);
        mGuideInfoRow = mActivity.findViewById(R.id.guideInfoRow);
        mChannels = mActivity.findViewById(R.id.channels);
        mTimeline = mActivity.findViewById(R.id.timeline);
        mProgramRows = mActivity.findViewById(R.id.programRows);
        mGuideSpinner = mActivity.findViewById(R.id.spinner);

        mProgramRows.setFocusable(false);
        mChannelScroller = mActivity.findViewById(R.id.channelScroller);
        ObservableScrollView programVScroller = mActivity.findViewById(R.id.programVScroller);
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x, y);
            }
        });

        mTimelineScroller = mActivity.findViewById(R.id.timelineHScroller);
        mTimelineScroller.setFocusable(false);
        mTimelineScroller.setFocusableInTouchMode(false);
        mTimeline.setFocusable(false);
        mTimeline.setFocusableInTouchMode(false);
        mChannelScroller.setFocusable(false);
        mChannelScroller.setFocusableInTouchMode(false);
        ObservableHorizontalScrollView programHScroller = mActivity.findViewById(R.id.programHScroller);
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

        // register to receive message from popup
        mActivity.registerMessageListener(new IMessageListener() {
            @Override
            public void onMessageReceived(CustomMessage message) {
                if (message.equals(CustomMessage.ActionComplete)) dismissProgramOptions();
            }
        });



        Intent intent = mActivity.getIntent();
        int startPos = intent.getIntExtra("Position", 0);

        // start playing
        mPlaybackController.play(startPos);

        mPlayPauseBtn.requestFocus();
    }

    private void setupPopupAnimations() {
        showPopup = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        showPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mPopupArea.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        hidePopup = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        hidePopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.setVisibility(View.GONE);
                mButtonRow.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void setupNextUpAnimations() {
        showNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        showNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mNextUpPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mNextButton.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        hideNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        hideNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mNextUpPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        showSmNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        showSmNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mSmNextUpPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSmNextButton.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        hideSmNextUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        hideSmNextUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSmNextUpPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChanged = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    mPlaybackController.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    // We don't do anything here on purpose
                    // On the Nexus we get this notification erroneously when first starting up
                    // and in any instance that we navigate away from our page, we already handle
                    // stopping video and handing back audio focus
                    break;
            }
        }
    };

    private OnItemViewClickedListener itemViewClickedListener = new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof BaseRowItem) {
                BaseRowItem rowItem = (BaseRowItem)item;
                switch (rowItem.getItemType()) {
                    case Chapter:
                        Long start = rowItem.getChapterInfo().getStartPositionTicks() / 10000;
                        mPlaybackController.seek(start);
                        hidePopupPanel();
                        break;
                }
            } else if (item instanceof ChannelInfoDto) {
                Utils.beep(100);
                hidePopupPanel();
                switchChannel(((ChannelInfoDto)item).getId());
            }
        }
    };

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP && mActivity != null && !mActivity.isFinishing()) {
                mActivity.finish();
                return true;
            }

            if (mPopupPanelVisible && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                // back should just hide the popup panel
                hidePopupPanel();

                // also close this if live tv
                if (mPlaybackController.isLiveTv()) hide();
                return true;
            }
            if (mGuideVisible) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    // go back to normal
                    hideGuide();
                    return true;
                } else if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) &&
                        (mSelectedProgram != null && mSelectedProgram.getChannelId() != null)) {
                    // tune to the current channel
                    Utils.beep();
                    switchChannel(mSelectedProgram.getChannelId());
                    return true;
                } else {
                    return false;
                }
            }

            if (mPlaybackController.isLiveTv() && keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                showGuide();
                return true;
            }

            if (mNextUpPanelVisible) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    //back should just hide the popup panel and show smaller one
                    hideNextUpPanel();
                    showSmNextUpPanel();
                    return true;
                }
                return false;
            }
            if (mSmNextUpPanelVisible) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    //back should just hide the popup panel
                    hideSmNextUpPanel();
                    return true;
                }
                return false;
            }
            if (mIsVisible && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                //back should just hide the panel
                hide();
                return true;
            }

            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_BUTTON_B && keyCode != KeyEvent.KEYCODE_ESCAPE) {
                if (mPopupPanelVisible) {
                    // up or down should close panel
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        hidePopupPanel();
                        if (mPlaybackController.isLiveTv()) hide(); //also close this if live tv
                        return true;
                    } else {
                        return false;
                    }
                }

                if (!mIsVisible) {
                    if (!DeviceUtils.isFireTv() && !mPlaybackController.isLiveTv()) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            Utils.beep(100);
                            mPlaybackController.skip(30000);
                            return true;
                        }

                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            Utils.beep(100);
                            mPlaybackController.skip(-11000);
                            return true;
                        }
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && mPlaybackController.isLiveTv()) {
                        showQuickChannelChanger();
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && mPlaybackController.canSeek()) {
                        mPlaybackController.pause();
                        return true;
                    }

                    //if we're not visible, show us
                    show();
                }

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();
            }

            return false;
        }
    };

    public long getCurrentLocalStartDate() { return mCurrentLocalGuideStart; }
    public long getCurrentLocalEndDate() { return mCurrentLocalGuideEnd; }

    private void switchChannel(String id) {
        if (id == null) return;
        if (mPlaybackController.getCurrentlyPlayingItem().getId().equals(id)) {
            // same channel, just dismiss overlay
            hideGuide();
        } else {
            mPlaybackController.stop();
            mCurrentProgress.setVisibility(View.VISIBLE);
            hideGuide();
            mApplication.getApiClient().GetItemAsync(id, mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    List<BaseItemDto> items = new ArrayList<BaseItemDto>();
                    items.add(response);
                    mPlaybackController.setItems(items);
                    mPlaybackController.play(0);
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast(mApplication, R.string.msg_video_playback_error);
                    finish();
                }
            });
        }
    }

    private void startFadeTimer() {
        mHandler.removeCallbacks(mHideTask);
        mHandler.postDelayed(mHideTask, 6000);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mApplication.getLogger().Error("Unable to get audio focus");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        // register a media button receiver so that all media button presses will come to us and not another app
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+

        if (TvApp.getApplication().isPlayingIntros()) {
            // don't show overlay
            TvApp.getApplication().setPlayingIntros(false);
        } else {
            if (!mIsVisible) {
                show(); // in case we were paused during video playback
                setFadingEnabled(true);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlaybackController != null) {
            mPlaybackController.removePreviousQueueItems();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPlaybackController.stop();

        // in case we come back
        setPlayPauseActionState(ImageButton.STATE_PRIMARY);

        // give back audio focus
        mAudioManager.abandonAudioFocus(mAudioFocusChanged);
        mApplication.getLogger().Debug("Fragment pausing. IsFinishing: %b", mActivity.isFinishing());
        if (!mActivity.isFinishing()) mActivity.finish(); // user hit "home" we want to back out
    }

    public void show() {
        hideInfo();
        mBottomPanel.startAnimation(slideUp);
        mTopPanel.startAnimation(slideDown);
        mIsVisible = true;
        mPlayPauseBtn.requestFocus();
    }

    public void hide() {
        mIsVisible = false;
        mBottomPanel.startAnimation(fadeOut);
        mTopPanel.startAnimation(fadeOut);
    }

    private void showChapterPanel() {
        setFadingEnabled(false);

        mPopupArea.startAnimation(showPopup);
        mPopupPanelVisible = true;
    }

    private void hidePopupPanel(){
        setFadingEnabled(true);
        mPopupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
    }

    private void showNextUpPanel() {
        if (mIsVisible) hide();
        if (mPopupPanelVisible) hidePopupPanel();

        mNextUpPanelVisible = true;
        mNextUpPanel.startAnimation(showNextUp);
    }

    private void hideNextUpPanel(){
        mNextUpPanel.startAnimation(hideNextUp);
        mNextUpPanelVisible = false;
    }

    private void showSmNextUpPanel() {
        if (mIsVisible) hide();
        if (mPopupPanelVisible) hidePopupPanel();

        mSmNextUpPanelVisible = true;
        mSmNextUpPanel.startAnimation(showSmNextUp);
    }

    private void hideSmNextUpPanel(){
        mSmNextUpPanel.startAnimation(hideSmNextUp);
        mSmNextUpPanelVisible = false;
    }

    private void showInfo() {
        mButtonRow.setVisibility(View.INVISIBLE);
        mCurrentProgress.setVisibility(View.INVISIBLE);
        mRemainingTime.setVisibility(View.INVISIBLE);
        mCurrentPos.setVisibility(View.INVISIBLE);
        mPlayPauseBtn.setVisibility(View.INVISIBLE);
        mInfoSummary.setVisibility(View.VISIBLE);
        setFadingEnabled(false);
    }

    private void hideInfo() {
        mButtonRow.setVisibility(View.VISIBLE);
        mCurrentProgress.setVisibility(View.VISIBLE);
        mRemainingTime.setVisibility(View.VISIBLE);
        mCurrentPos.setVisibility(View.VISIBLE);
        mPlayPauseBtn.setVisibility(View.VISIBLE);
        mInfoSummary.setVisibility(View.INVISIBLE);
    }

    private void showGuide() {
        hide();
        mPlaybackController.mVideoManager.contractVideo(Utils.convertDpToPixel(mActivity, 300));
        mTvGuide.setVisibility(View.VISIBLE);
        mGuideVisible = true;
        Calendar now = Calendar.getInstance();
        boolean needLoad = mCurrentGuideStart == null;
        if (!needLoad) {
            Calendar needLoadTime = (Calendar) mCurrentGuideStart.clone();
            needLoadTime.add(Calendar.MINUTE, 30);
            needLoad = now.after(needLoadTime);
        }
        if (needLoad) {
            loadGuide();
        }
    }

    private void hideGuide() {
        mTvGuide.setVisibility(View.GONE);
        mPlaybackController.mVideoManager.setVideoFullSize();
        mGuideVisible = false;
    }

    private void loadGuide() {
        mGuideSpinner.setVisibility(View.VISIBLE);
        fillTimeLine(mGuideHours);
        if (mAllChannels == null) {
            TvManager.loadAllChannels(new Response<Integer>() {
                @Override
                public void onResponse(Integer ndx) {
                    if (ndx >= PAGE_SIZE) {
                        // last channel is not in first page so grab a set where it will be in the middle
                        ndx = ndx - (PAGE_SIZE / 2);
                    } else {
                        ndx = 0; // just start at beginning
                    }

                    mAllChannels = TvManager.getAllChannels();
                    if (mAllChannels.size() > 0) {
                        displayChannels(ndx, PAGE_SIZE);
                    } else {
                        mGuideSpinner.setVisibility(View.GONE);
                    }
                }
            });

        }
    }

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) end = mAllChannels.size();

        mCurrentDisplayChannelStartNdx = start;
        mCurrentDisplayChannelEndNdx = end - 1;
        TvApp.getApplication().getLogger().Debug("*** Display channels pre-execute");
        mGuideSpinner.setVisibility(View.VISIBLE);

        mChannels.removeAllViews();
        mProgramRows.removeAllViews();
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
        View firstRow;
        int displayedChannels = 0;

        @Override
        protected void onPreExecute() {
            TvApp.getApplication().getLogger().Debug("*** Display programs pre-execute");
            mChannels.removeAllViews();
            mProgramRows.removeAllViews();
            mFirstFocusChannelId = mPlaybackController.getCurrentlyPlayingItem().getId();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) pageUpStart = 0;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(LiveTvGuideActivity.PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);
                displayedChannels = 0;

                mProgramRows.addView(new GuidePagingButton(mActivity, mFragment, pageUpStart, getString(R.string.lbl_load_channels)+mAllChannels.get(pageUpStart).getNumber() + " - "+mAllChannels.get(mCurrentDisplayChannelStartNdx-1).getNumber()));
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
                List<BaseItemDto> programs = TvManager.getProgramsForChannel(channel.getId());
                final LinearLayout row = getProgramRow(programs, channel.getId());
                if (first) {
                    first = false;
                    firstRow = row;
                }

                // put focus on the last tuned channel
                if (channel.getId().equals(mFirstFocusChannelId)) {
                    firstRow = row;
                    mFirstFocusChannelId = null; // only do this first time in not while paging around
                }

                // set focus parameters if we are not on first row
                // this makes focus movements more predictable for the grid view
                if (prevRow != null) {
                    TvManager.setFocusParms(row, prevRow, true);
                    TvManager.setFocusParms(prevRow, row, false);
                }
                prevRow = row;

                mActivity.runOnUiThread(new Runnable() {
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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TvApp.getApplication().getLogger().Debug("*** Display programs post execute");
            if (mCurrentDisplayChannelEndNdx < mAllChannels.size()-1) {
                // Show a paging row for channels below
                int pageDnEnd = mCurrentDisplayChannelEndNdx + PAGE_SIZE;
                if (pageDnEnd >= mAllChannels.size()) pageDnEnd = mAllChannels.size()-1;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(LiveTvGuideActivity.PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);

                mProgramRows.addView(new GuidePagingButton(mActivity, mFragment, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels)+mAllChannels.get(mCurrentDisplayChannelEndNdx+1).getNumber() + " - "+mAllChannels.get(pageDnEnd).getNumber()));
            }

            mChannelStatus.setText(displayedChannels+" of "+mAllChannels.size()+" channels");
            mFilterStatus.setText(" for next "+ mGuideHours+" hours");
            mFilterStatus.setTextColor(Color.GRAY);

            mGuideSpinner.setVisibility(View.GONE);
            if (firstRow != null) firstRow.requestFocus();
        }
    }

    private int currentCellId = 0;

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {
        LinearLayout programRow = new LinearLayout(mActivity);
        if (programs.size() == 0) {
            BaseItemDto empty = new BaseItemDto();
            empty.setName(mApplication.getString(R.string.no_program_data));
            empty.setChannelId(channelId);
            empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart)));
            empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + (150 * 60000))));
            ProgramGridCell cell = new ProgramGridCell(mActivity, mFragment, empty);
            cell.setId(currentCellId++);
            cell.setLayoutParams(new ViewGroup.LayoutParams(150 * PIXELS_PER_MINUTE, LiveTvGuideActivity.ROW_HEIGHT));
            cell.setFocusable(true);
            programRow.addView(cell);
            return programRow;
        }

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? TimeUtils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) start = getCurrentLocalStartDate();
            if (start > getCurrentLocalEndDate()) continue;
            if (start > prevEnd) {
                // fill empty time slot
                BaseItemDto empty = new BaseItemDto();
                empty.setName("  <No Program Data Available>");
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
                Long duration = (start - prevEnd);
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd + duration)));
                ProgramGridCell cell = new ProgramGridCell(mActivity, mFragment, empty);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long) (duration / 60000)).intValue() * PIXELS_PER_MINUTE, LiveTvGuideActivity.ROW_HEIGHT));
                cell.setFocusable(true);
                programRow.addView(cell);
            }
            long end = item.getEndDate() != null ? TimeUtils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end - start) / 60000;
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(mActivity, mFragment, item);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * PIXELS_PER_MINUTE, LiveTvGuideActivity.ROW_HEIGHT));
                program.setFocusable(true);

                programRow.addView(program);
            }
        }

        return programRow;
    }

    private void fillTimeLine(int hours) {
        mCurrentGuideStart = Calendar.getInstance();
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
            TextView time = new TextView(mActivity);
            time.setText(android.text.format.DateFormat.getTimeFormat(mActivity).format(current.getTime()));
            time.setWidth(interval == 30 ? halfHour : oneHour);
            mTimeline.addView(time);
            current.add(Calendar.MINUTE, interval);
            //after first one, we always go on hours
            interval = 60;
        }
    }

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
        mGuideTitle.setText(mSelectedProgram.getName());
        mSummary.setText(mSelectedProgram.getOverview());
        if (mSelectedProgram.getId() != null) {
            mDisplayDate.setText(TimeUtils.getFriendlyDate(TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));

            //info row
            InfoLayoutHelper.addInfoRow(mActivity, mSelectedProgram, mGuideInfoRow, false, false);
        } else {
            mGuideInfoRow.removeAllViews();
        }

    }

    public void setSelectedProgram(ProgramGridCell programView) {
        mSelectedProgramView = programView;
        mSelectedProgram = programView.getProgram();
        mHandler.removeCallbacks(detailUpdateTask);
        mHandler.postDelayed(detailUpdateTask, 500);
    }

    public void dismissProgramOptions() {
        if (mDetailPopup != null) mDetailPopup.dismiss();
    }

    private LiveProgramDetailPopup mDetailPopup;
    public void showProgramOptions() {
        if (mSelectedProgram == null) return;
        if (mDetailPopup == null) mDetailPopup = new LiveProgramDetailPopup(mActivity, Utils.convertDpToPixel(mActivity, 600), new EmptyResponse() {
            @Override
            public void onResponse() {
                switchChannel(mSelectedProgram.getChannelId());
            }
        });
        mDetailPopup.setContent(mSelectedProgram, mSelectedProgramView);
        mDetailPopup.show(mGuideTitle, mTitle.getLeft(), mGuideTitle.getTop() - 10);

    }

    private Animation.AnimationListener hideAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTopPanel.setVisibility(View.GONE);
            mBottomPanel.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private Animation.AnimationListener showAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTopPanel.setVisibility(View.VISIBLE);
            mBottomPanel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void updatePoster(BaseItemDto item, ImageView target, boolean preferSeries) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int height = Utils.convertDpToPixel(getActivity(), 300);
            int width = Utils.convertDpToPixel(getActivity(), 150);
            String posterImageUrl = ImageUtils.getPrimaryImageUrl(item, mApplication.getApiClient(), false, false, preferSeries, height);
            if (posterImageUrl != null) Picasso.with(getActivity()).load(posterImageUrl).skipMemoryCache().resize(width, height).centerInside().into(target);
        }
    }

    private void updateLogo(BaseItemDto item, ImageView target) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int height = Utils.convertDpToPixel(getActivity(), 60);
            int width = Utils.convertDpToPixel(getActivity(), 180);
            String imageUrl = ImageUtils.getLogoImageUrl(item, mApplication.getApiClient());
            if (imageUrl != null) Picasso.with(getActivity()).load(imageUrl).skipMemoryCache().resize(width, height).centerInside().into(target);
        }
    }

    private void updateStudio(BaseItemDto item) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int height = Utils.convertDpToPixel(mActivity, 30);
            int width = Utils.convertDpToPixel(mActivity, 70);
            if (item.getStudios() != null && item.getStudios().length > 0 && item.getStudios()[0].getHasPrimaryImage()) {
                String studioImageUrl = ImageUtils.getPrimaryImageUrl(item.getStudios()[0], mApplication.getApiClient(), height);
                if (studioImageUrl != null)
                    Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().into(mStudioImage);
            } else {
                if (item.getSeriesStudio() != null) {
                    String studioImageUrl = null;
                    try {
                        ImageOptions options = new ImageOptions();
                        options.setMaxHeight(height);
                        options.setImageType(ImageType.Primary);
                        studioImageUrl = mApplication.getApiClient().GetStudioImageUrl(URLEncoder.encode(item.getSeriesStudio(), "utf-8"), options);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (studioImageUrl != null)
                        Picasso.with(mActivity).load(studioImageUrl).resize(width, height).centerInside().into(mStudioImage);
                } else {
                    mStudioImage.setImageResource(R.drawable.blank30x30);
                }
            }
        }
    }

    public void updateEndTime(final long timeLeft) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEndTime.setText(timeLeft > 0 ?
                                mApplication.getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(System.currentTimeMillis() + timeLeft)
                                : ""
                );
            }
        });
    }

    private void updateCurrentDuration(BaseItemDto item) {
        Long mbRuntime = item.getRunTimeTicks();
        Long andDuration = mbRuntime != null ? mbRuntime / 10000: 0;
        mCurrentDuration = andDuration.intValue();
    }

    private void showQuickChannelChanger() {
        showChapterPanel();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ndx = TvManager.getAllChannelsIndex(TvManager.getLastLiveTvChannel());
                if (ndx > 0) {
                    mPopupRowPresenter.setPosition(ndx);
                }
            }
        },500);
    }

    private void addButtons(BaseItemDto item) {
        mButtonRow.removeAllViews();

        if (!DeviceUtils.isFireTv() && mPlaybackController.canSeek()) {
            // on-screen jump buttons for Nexus
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_loop, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.skip(-11000);
                    startFadeTimer();
                }
            }));

            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_fastforward, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.skip(30000);
                    startFadeTimer();
                }
            }));
        }

        if (mPlaybackController.isLiveTv()) {
            // previous channel button
            if (TvManager.getPrevLiveTvChannel() != null) {
                mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_previous_episode, mButtonSize, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchChannel(TvManager.getPrevLiveTvChannel());
                    }
                }));
            }

            // create quick channel change row
            TvManager.loadAllChannels(new Response<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    ArrayObjectAdapter channelAdapter = new ArrayObjectAdapter(new ChannelCardPresenter());
                    channelAdapter.addAll(0, TvManager.getAllChannels());
                    if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
                    mChapterRow = new ListRow(new HeaderItem("Channels"), channelAdapter);
                    mPopupRowAdapter.add(mChapterRow);
                }
            });

            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_channel_bar, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showQuickChannelChanger();
                }
            }));

            // guide button
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_guide, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGuide();
                }
            }));

            // record button
            if (item.getCurrentProgram() != null && mApplication.canManageRecordings()) {
                mButtonRow.addView(new ImageButton(mActivity, item.getCurrentProgram().getTimerId() != null ? R.drawable.ic_record_red : R.drawable.ic_record, mButtonSize, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleRecording(mPlaybackController.getCurrentlyPlayingItem());
                    }
                }));
            }

            // adjust popup for channels
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mPopupArea.getLayoutParams();
            params.height = Utils.convertDpToPixel(mActivity, 170);
            mPopupArea.setLayoutParams(params);
        }

        if (!TextUtils.isEmpty(item.getOverview())) {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_info, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showInfo();
                }
            }));
        }

        boolean hasSubs = StreamHelper.getSubtitleStreams(mPlaybackController.getCurrentMediaSource()).size() > 0;
        boolean hasMultiAudio = StreamHelper.getAudioStreams(mPlaybackController.getCurrentMediaSource()).size() > 1;

        if (hasMultiAudio) {
            mApplication.getLogger().Debug("Multiple Audio tracks found: %d", StreamHelper.getAudioStreams(mPlaybackController.getCurrentMediaSource()).size());
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_select_audio, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlaybackController.getCurrentStreamInfo() == null) {
                        TvApp.getApplication().getLogger().Warn("StreamInfo null trying to obtain audio tracks");
                        Utils.showToast(TvApp.getApplication(), "Unable to obtain audio track info");
                        return;
                    }
                    setFadingEnabled(false);

                    List<MediaStream> audioTracks = TvApp.getApplication().getPlaybackManager().getInPlaybackSelectableAudioStreams(mPlaybackController.getCurrentStreamInfo());
                    Integer currentAudioIndex = mPlaybackController.getAudioStreamIndex();
                    if (!mPlaybackController.isNativeMode() && currentAudioIndex > audioTracks.size()) {
                        //VLC has translated this to an ID - we need to translate back to our index positionally
                        currentAudioIndex = mPlaybackController.translateVlcAudioId(currentAudioIndex);
                    }

                    PopupMenu audioMenu = Utils.createPopupMenu(getActivity(), v, Gravity.END);
                    for (MediaStream audio : audioTracks) {
                        MenuItem item = audioMenu.getMenu().add(0, audio.getIndex(), audio.getIndex(), audio.getDisplayTitle());
                        if (currentAudioIndex != null && currentAudioIndex == audio.getIndex()) item.setChecked(true);
                    }
                    audioMenu.getMenu().setGroupCheckable(0, true, false);
                    audioMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            setFadingEnabled(true);
                        }
                    });
                    audioMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mApplication.getLogger().Debug("Selected stream %s", item.getTitle().toString());
                            mPlaybackController.switchAudioStream(item.getItemId());
                            return true;
                        }
                    });
                    audioMenu.show();
                }
            }));
        } else {
            mApplication.getLogger().Debug("Only one audio track.");
        }

        if (hasSubs) {
            mApplication.getLogger().Debug("Subtitle tracks found: %d", mPlaybackController.getSubtitleStreams().size());
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_select_subtitle, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlaybackController.getCurrentStreamInfo() == null) {
                        TvApp.getApplication().getLogger().Warn("StreamInfo null trying to obtain subtitles");
                        Utils.showToast(TvApp.getApplication(), "Unable to obtain subtitle info");
                        return;
                    }
                    setFadingEnabled(false);
                    List<SubtitleStreamInfo> subtitles = mPlaybackController.getSubtitleStreams();
                    PopupMenu subMenu = Utils.createPopupMenu(getActivity(), v, Gravity.END);
                    MenuItem none = subMenu.getMenu().add(0, -1, 0, mApplication.getString(R.string.lbl_none));
                    int currentSubIndex = mPlaybackController.getSubtitleStreamIndex();
                    if (currentSubIndex < 0) none.setChecked(true);
                    for (SubtitleStreamInfo sub : subtitles) {
                        MenuItem item = subMenu.getMenu().add(0, sub.getIndex(), sub.getIndex(), sub.getDisplayTitle());
                        if (currentSubIndex == sub.getIndex()) item.setChecked(true);
                    }
                    subMenu.getMenu().setGroupCheckable(0, true, false);
                    subMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            setFadingEnabled(true);
                        }
                    });
                    subMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mApplication.getLogger().Debug("Selected subtitle %s", item.getTitle().toString());
                            mPlaybackController.switchSubtitleStream(item.getItemId());
                            return true;
                        }
                    });
                    subMenu.show();
                }
            }));
        } else {
            mApplication.getLogger().Debug("No sub tracks found.");
        }

        List<ChapterInfoDto> chapters = item.getChapters();
        if (chapters != null && chapters.size() > 0) {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_select_chapter, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChapterPanel();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int ndx = getCurrentChapterIndex(mPlaybackController.getCurrentlyPlayingItem(), mPlaybackController.getCurrentPosition() * 10000);
                            if (ndx > 0) {
                                mPopupRowPresenter.setPosition(ndx);
                            }
                        }
                    },500);
                }
            }));

            // create chapter row for later use
            ItemRowAdapter chapterAdapter = new ItemRowAdapter(BaseItemUtils.buildChapterItems(item), new CardPresenter(true, 220), new ArrayObjectAdapter());
            chapterAdapter.Retrieve();
            if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
            mChapterRow = new ListRow(new HeaderItem(mActivity.getString(R.string.chapters)), chapterAdapter);
            mPopupRowAdapter.add(mChapterRow);

        }

        if (mPlaybackController.hasNextItem()) {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.lb_ic_skip_next, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlaybackController.next();
                }
            }));
        }

        if (!mPlaybackController.isNativeMode()) {
            if (mAudioPopup == null ) mAudioPopup = new AudioDelayPopup(mActivity, mBottomPanel, new ValueChangedListener<Long>() {
                @Override
                public void onValueChanged(Long value) {
                    mPlaybackController.setAudioDelay(value);
                }
            });
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_adjust, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAudioPopup.show(mPlaybackController.getAudioDelay());
                }
            }));
        } else {
            mButtonRow.addView(new ImageButton(mActivity, R.drawable.ic_zoom, mButtonSize, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu zoomMenu = Utils.createPopupMenu(mActivity, v, Gravity.RIGHT);
                    zoomMenu.getMenu().add(0, VideoManager.ZOOM_NORMAL, VideoManager.ZOOM_NORMAL, mApplication.getString(R.string.lbl_normal)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_NORMAL);
                    zoomMenu.getMenu().add(0, VideoManager.ZOOM_VERTICAL, VideoManager.ZOOM_VERTICAL, mApplication.getString(R.string.lbl_vertical_stretch)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_VERTICAL);
                    zoomMenu.getMenu().add(0, VideoManager.ZOOM_HORIZONTAL, VideoManager.ZOOM_HORIZONTAL, mApplication.getString(R.string.lbl_horizontal_stretch)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_HORIZONTAL);
                    zoomMenu.getMenu().add(0, VideoManager.ZOOM_FULL, VideoManager.ZOOM_FULL, mApplication.getString(R.string.lbl_zoom)).setChecked(mPlaybackController.getZoomMode() == VideoManager.ZOOM_FULL);

                    zoomMenu.getMenu().setGroupCheckable(0, true, false);
                    zoomMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            setFadingEnabled(true);
                        }
                    });
                    zoomMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mPlaybackController.setZoom(item.getItemId());
                            return true;
                        }
                    });

                    setFadingEnabled(false);
                    zoomMenu.show();
                }
            }));
        }
    }

    private int getCurrentChapterIndex(BaseItemDto item, long pos) {
        int ndx = 0;
        TvApp.getApplication().getLogger().Debug("*** looking for chapter at pos: %d", pos);
        if (item.getChapters() != null) {
            for (ChapterInfoDto chapter : item.getChapters()) {
                TvApp.getApplication().getLogger().Debug("*** chapter %d has pos: %d", ndx, chapter.getStartPositionTicks());
                if (chapter.getStartPositionTicks() > pos) return ndx - 1;
                ndx++;
            }
        }
        return ndx - 1;
    }

    private void toggleRecording(BaseItemDto item) {
        final BaseItemDto program = item.getCurrentProgram();

        if (program != null) {
            if (program.getTimerId() != null) {
                // cancel
                if (program.getSeriesTimerId() != null) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.lbl_cancel_recording)
                            .setMessage(R.string.msg_cancel_entire_series)
                            .setPositiveButton(R.string.lbl_cancel_series, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancelRecording(program, true);
                                }
                            })
                            .setNegativeButton("Just this one", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancelRecording(program, false);
                                }
                            })
                            .show();
                } else {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.lbl_cancel_recording)
                            .setPositiveButton(R.string.lbl_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancelRecording(program, false);
                                }
                            })
                            .setNegativeButton(R.string.lbl_no, null)
                            .show();
                }
            } else {
                if (Utils.isTrue(program.getIsSeries())) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.lbl_record_series)
                            .setMessage(R.string.msg_record_entire_series)
                            .setPositiveButton(R.string.lbl_record_series, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recordProgram(program, true);
                                }
                            })
                            .setNegativeButton(R.string.lbl_just_this_once, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recordProgram(program, false);
                                }
                            })
                            .show();
                } else {
                    recordProgram(program, false);
                }
            }
        }
    }

    private void cancelRecording(BaseItemDto program, boolean series) {
        if (program != null) {
            if (series) {
                mApplication.getApiClient().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyResponse() {
                    @Override
                    public void onResponse() {
                        Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                        mPlaybackController.updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                    }
                });
            } else {
                mApplication.getApiClient().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
                    @Override
                    public void onResponse() {
                        Utils.showToast(mActivity, R.string.msg_recording_cancelled);
                        mPlaybackController.updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        Utils.showToast(mActivity, R.string.msg_unable_to_cancel);
                    }
                });
            }
        }
    }

    private void recordProgram(final BaseItemDto program, final boolean series) {
        if (program != null) {
            mApplication.getApiClient().GetDefaultLiveTvTimerInfo(new Response<SeriesTimerInfoDto>() {
                @Override
                public void onResponse(SeriesTimerInfoDto response) {
                    response.setProgramId(program.getId());
                    if (series) {
                        mApplication.getApiClient().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                Utils.showToast(mActivity, R.string.msg_set_to_record);
                                mPlaybackController.updateTvProgramInfo();
                                TvManager.forceReload();
                            }

                            @Override
                            public void onError(Exception ex) {
                                Utils.showToast(mActivity, R.string.msg_unable_to_create_recording);
                            }
                        });
                    } else {
                        mApplication.getApiClient().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                Utils.showToast(mActivity, R.string.msg_set_to_record);
                                mPlaybackController.updateTvProgramInfo();
                                TvManager.forceReload();
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
    }

    AudioDelayPopup mAudioPopup;

    @Override
    public void setCurrentTime(long time) {
        if (mNextUpPanelVisible) {
            mStartsIn.setText(mCurrentDuration > 0 ? "Starts in " + TimeUtils.formatMillis(mCurrentDuration - time) : "");
        } else if (mSmNextUpPanelVisible) {
            mSmStartsIn.setText(mCurrentDuration > 0 ? "Starts in " + TimeUtils.formatMillis(mCurrentDuration - time) : "");
        } else {
            mCurrentProgress.setProgress(((Long)time).intValue());
            mCurrentPos.setText(TimeUtils.formatMillis(time));
            mRemainingTime.setText(mCurrentDuration > 0 ? "-" + TimeUtils.formatMillis(mCurrentDuration - time) : "");
        }
    }

    @Override
    public void setSecondaryTime(long time) {
        mCurrentProgress.setSecondaryProgress(((Long)time).intValue());
    }

    @Override
    public void setFadingEnabled(boolean value) {
        mFadeEnabled = value;
        if (mFadeEnabled) {
            if (mIsVisible) startFadeTimer();
        } else {
            mHandler.removeCallbacks(mHideTask);
            if (!mIsVisible) getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            });
        }
    }

    @Override
    public void setPlayPauseActionState(final int state) {
        if (getActivity() != null && !getActivity().isFinishing()) getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlayPauseBtn.setState(state);
            }
        });
    }

    @Override
    public void updateDisplay() {
        BaseItemDto current = mPlaybackController.getCurrentlyPlayingItem();
        if (current != null & mActivity != null && !mActivity.isFinishing()) {
            if (mNextUpPanelVisible) hideNextUpPanel();
            if (mSmNextUpPanelVisible) hideSmNextUpPanel();
            updateCurrentDuration(current);
            // set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            // set other information
            mTitle.setText(current.getName());
            mInfoSummary.setText(current.getOverview());
            mGuideCurrentTitle.setText(current.getName());
            updatePoster(current, mPoster, true);
            updateLogo(current, mLogoImage);
            updateStudio(current);
            addButtons(current);
            InfoLayoutHelper.addInfoRow(mActivity, current, mInfoRow, true, false, mPlaybackController.getCurrentMediaSource().GetDefaultAudioStream(mPlaybackController.getAudioStreamIndex()));

            if (mApplication.getPrefs().getBoolean("pref_enable_debug", false)) {
                StreamInfo stream = mPlaybackController.getCurrentStreamInfo();
                if (stream != null) {
                    switch (stream.getPlayMethod()) {
                        case Transcode:
                            InfoLayoutHelper.addBlockText(mActivity, mInfoRow, "Trans" + (mPlaybackController.mVideoManager.isNativeMode() ? "/I" : "/V"));
                            break;
                        case DirectStream:
                        case DirectPlay:
                            InfoLayoutHelper.addBlockText(mActivity, mInfoRow, "Direct" + (mPlaybackController.mVideoManager.isNativeMode() ? "/I" : "/V"));
                            break;
                    }
                }
            }

            if (mIsVisible) mPlayPauseBtn.requestFocus();
        }
    }

    @Override
    public void nextItemThresholdHit(BaseItemDto nextItem) {
        mApplication.getLogger().Debug("Next Item is %s", nextItem.getName());
        // need to retrieve full item for all info
        mApplication.getApiClient().GetItemAsync(nextItem.getId(), mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                mNextUpTitle.setText("Up Next...  " + response.getName());
                mSmNextUpTitle.setText("Up Next...  " + response.getName());
                mNextUpSummary.setText(response.getOverview());
                InfoLayoutHelper.addInfoRow(mActivity, response, mNextUpInfoRow, true, true);
                updatePoster(response, mNextUpPoster, true);
                showNextUpPanel();
            }
        });
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    private SubtitleTrackInfo mManualSubs;
    private long lastReportedPosMs;

    private void updateManualSubtitlePosition() {
        // adjust subtitles margin based on screen dimension
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSubtitleText.getLayoutParams();
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        params.topMargin = (dm.heightPixels / 2) - 140;
        params.rightMargin = params.leftMargin = dm.widthPixels / 4;
        mSubtitleText.setLayoutParams(params);
    }

    public void addManualSubtitles(SubtitleTrackInfo info) {
        mManualSubs = info;
        lastReportedPosMs = 0;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSubtitleText.setVisibility(View.INVISIBLE);
                mSubtitleText.setText("");
            }
        });
    }

    public void showSubLoadingMsg(final boolean show) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    mSubtitleText.setText(R.string.msg_subtitles_loading);
                    mSubtitleText.setVisibility(View.VISIBLE);
                } else {
                    mSubtitleText.setVisibility(View.INVISIBLE);
                    mSubtitleText.setText("");
                }
            }
        });
    }

    public void updateSubtitles(long positionMs) {
        if (lastReportedPosMs > 0){
            if (Math.abs(lastReportedPosMs - positionMs) < 500) {
                return;
            }
        }

        if (mManualSubs == null) {
            return;
        }

        long positionTicks = positionMs * 10000;
        for (SubtitleTrackEvent caption : mManualSubs.getTrackEvents()) {
            if (positionTicks >= caption.getStartPositionTicks() && positionTicks <= caption.getEndPositionTicks()) {
                setTimedText(caption);
                return;
            }
        }

        setTimedText(null);
    }

    private void setTimedText(final SubtitleTrackEvent textObj) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textObj == null) {
                    mSubtitleText.setVisibility(View.INVISIBLE);
                    return;
                }

                String text = textObj.getText();
                if (text == null || text.length() == 0) {
                    mSubtitleText.setVisibility(View.INVISIBLE);
                    return;
                }

                SpannableString span = new SpannableString(Html.fromHtml(text));
                span.setSpan(new ForegroundColorSpan(Color.WHITE), 0, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                mSubtitleText.setText(span);
                mSubtitleText.setVisibility(View.VISIBLE);
            }
        });
    }
}
