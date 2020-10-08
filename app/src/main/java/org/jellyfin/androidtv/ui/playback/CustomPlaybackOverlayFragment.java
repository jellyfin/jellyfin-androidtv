package org.jellyfin.androidtv.ui.playback;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.constant.CustomMessage;
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
import org.jellyfin.androidtv.ui.shared.IMessageListener;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.ILiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideActivity;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.nextup.NextUpActivity;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.ChannelCardPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.DeviceUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.RemoteControlReceiver;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.ChapterInfoDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackEvent;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.inject;

public class CustomPlaybackOverlayFragment extends Fragment implements IPlaybackOverlayFragment, ILiveTvGuide {
    ImageView mLogoImage;
    View mTopPanel;
    FrameLayout mPopupArea;
    RowsSupportFragment mPopupRowsFragment;
    ListRow mChapterRow;
    ArrayObjectAdapter mPopupRowAdapter;
    PositionableListRowPresenter mPopupRowPresenter;
    CustomPlaybackOverlayFragment mFragment;
    TextView mSubtitleText;

    //Live guide items
    public static final int PIXELS_PER_MINUTE = Utils.convertDpToPixel(TvApp.getApplication(), 7);
    public static final int PAGE_SIZE = 75;
    RelativeLayout mTvGuide;
    private RelativeLayout mChannelNumberView;
    private TextView mChannelNumberTextView;
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
    private ObservableScrollView mChannelScroller;
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
    private int mGuideHours = 9;
    private List<ChannelInfoDto> mAllChannels;
    private String mFirstFocusChannelId;
    private int mChannelOffset = 0;

    PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay;

    Animation fadeOut;
    Animation slideUp;
    Animation slideDown;
    Animation showPopup;
    Animation hidePopup;
    Handler mHandler = new Handler();
    Runnable mHideTask;
    private Runnable mHideNumberTask;

    TvApp mApplication;
    PlaybackOverlayActivity mActivity;
    private AudioManager mAudioManager;

    int mButtonSize;

    boolean mFadeEnabled = false;
    boolean mIsVisible = false;
    boolean mPopupPanelVisible = false;
    private boolean mChannelNumberVisible = false;
    private String mChannelNumber = "";

    int mCurrentDuration;
    private LeanbackOverlayFragment leanbackOverlayFragment;
    private VideoManager videoManager = null;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

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
            Timber.e("Unable to get audio manager");
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
                if (mIsVisible) {
                    hide();
                    leanbackOverlayFragment.hideOverlay();
                }
            }
        };

        mHideNumberTask = new Runnable() {
            @Override
            public void run() {
                if (mChannelNumberVisible) {
                    //Hide channel number text view
                    switchChannelByNumber(mChannelNumber);
                    hideChannelNumberView();
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.vlc_player_interface, container, false);

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
        mChannelNumberView = (RelativeLayout) inflater.inflate(R.layout.guide_channel_number, null);

        root.addView(mChannelNumberView);
        root.addView(mTvGuide);
        mTvGuide.setVisibility(View.GONE);
        mChannelNumberView.setVisibility(View.GONE);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //if we're not visible, show us
                if (!mIsVisible) show();

                //and then manage our fade timer
                if (mFadeEnabled) startFadeTimer();

                Timber.d("Got touch event.");
                return false;
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (TvApp.getApplication().getPlaybackController() != null) {
            videoManager = new VideoManager(mActivity, view);
            TvApp.getApplication().getPlaybackController().init(videoManager);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (videoManager != null)
            videoManager.destroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) return;

        prepareOverlayFragment();
        mLogoImage = mActivity.findViewById(R.id.logoImage);
        mTopPanel = mActivity.findViewById(R.id.topPanel);

        mPopupArea = mActivity.findViewById(R.id.popupArea);

        //manual subtitles
        mSubtitleText = mActivity.findViewById(R.id.offLine_subtitleText);

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(mActivity, R.anim.abc_slide_in_bottom);
        slideDown.setAnimationListener(showAnimationListener);
        setupPopupAnimations();

        //live guide
        mDisplayDate = mActivity.findViewById(R.id.displayDate);
        mGuideTitle = mActivity.findViewById(R.id.guideTitle);
        mGuideCurrentTitle = mActivity.findViewById(R.id.guideCurrentTitle);
        mSummary = mActivity.findViewById(R.id.summary);
        mChannelStatus = mActivity.findViewById(R.id.channelsStatus);
        mFilterStatus = mActivity.findViewById(R.id.filterStatus);
        mChannelStatus.setTextColor(Color.GRAY);
        mFilterStatus.setTextColor(Color.GRAY);
        mGuideInfoRow = mActivity.findViewById(R.id.guideInfoRow);
        mChannels = mActivity.findViewById(R.id.channels);
        mTimeline = mActivity.findViewById(R.id.timeline);
        mProgramRows = mActivity.findViewById(R.id.programRows);
        mGuideSpinner = mActivity.findViewById(R.id.spinner);
        mChannelNumberTextView = mActivity.findViewById(R.id.guide_channel_number);

        mProgramRows.setFocusable(false);
        mChannelScroller = mActivity.findViewById(R.id.channelScroller);
        ObservableScrollView programVScroller = mActivity.findViewById(R.id.programVScroller);
        programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                mChannelScroller.scrollTo(x, y);
            }
        });

        mChannelScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                programVScroller.scrollTo(x, y);
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

        mActivity.getOnBackPressedDispatcher().addCallback(backPressedCallback);


        Intent intent = mActivity.getIntent();
        int startPos = intent.getIntExtra("Position", 0);

        // start playing
        mPlaybackController.play(startPos);
        leanbackOverlayFragment.updatePlayState();

    }

    private void prepareOverlayFragment() {
        leanbackOverlayFragment = (LeanbackOverlayFragment) getChildFragmentManager().findFragmentById(R.id.leanback_fragment);
        if (leanbackOverlayFragment != null) {
            leanbackOverlayFragment.initFromView(mPlaybackController, this);
            leanbackOverlayFragment.mediaInfoChanged();
            leanbackOverlayFragment.setOnKeyInterceptListener(keyListener);
        }
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
                BaseRowItem rowItem = (BaseRowItem) item;
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
                switchChannel(((ChannelInfoDto) item).getId());
            }
        }
    };

    private OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {

        @Override
        public void handleOnBackPressed() {
            if (mPopupPanelVisible) {
                // back should just hide the popup panel
                hidePopupPanel();
                leanbackOverlayFragment.hideOverlay();

                // also close this if live tv
                if (mPlaybackController.isLiveTv()) hide();
            } else if (mGuideVisible) {
                hideGuide();
            } else if (mChannelNumberVisible) {
                mHandler.removeCallbacks(mHideNumberTask);
                hideChannelNumberView();
            } else {
                mActivity.finish();
            }
        }
    };

    public boolean onKeyUp(int keyCode, KeyEvent event){
        if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
            if (mGuideVisible && mSelectedProgram != null && mSelectedProgram.getChannelId() != null) {
                Date curUTC = TimeUtils.convertToUtcDate(new Date());
                if (mSelectedProgram.getStartDate().before(curUTC))
                    switchChannel(mSelectedProgram.getChannelId());
                else
                    showProgramOptions();
                return true;
            }
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event){
        showProgramOptions();
        return true;
    }

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {

                if (mPlaybackController.isLiveTv()) {
                    if (mChannelNumberVisible && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        mHandler.removeCallbacks(mHideNumberTask);
                        switchChannelByNumber(mChannelNumber);
                        hideChannelNumberView();
                        return true;
                    }

                    if (!mGuideVisible) {
                        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                            leanbackOverlayFragment.setShouldShowOverlay(false);
                            if (mPopupPanelVisible)
                                hidePopupPanel();
                            if (mIsVisible)
                                leanbackOverlayFragment.hideOverlay();

                            mChannelNumber += String.valueOf(keyCode - 7);
                            mChannelNumberVisible = true;
                            mChannelNumberView.setVisibility(View.VISIBLE);
                            mChannelNumberTextView.setText(mChannelNumber);

                            mHandler.removeCallbacks(mHideNumberTask);
                            mHandler.postDelayed(mHideNumberTask, 5000);
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_LAST_CHANNEL) {
                            switchChannel(TvManager.getPrevLiveTvChannel());
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                            leanbackOverlayFragment.setShouldShowOverlay(false);
                            channelButton(false);
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                            leanbackOverlayFragment.setShouldShowOverlay(false);
                            channelButton(true);
                            return true;
                        }
                    }
                }
                Timber.d("Key Code: " + String.valueOf(keyCode));

                //166 up
                //167 down

                if (!mGuideVisible)
                    leanbackOverlayFragment.setShouldShowOverlay(true);
                else {
                    leanbackOverlayFragment.setShouldShowOverlay(false);
                    leanbackOverlayFragment.hideOverlay();
                }

                if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP && mActivity != null && !mActivity.isFinishing()) {
                    mActivity.finish();
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    if (mPopupPanelVisible) {
                        // back should just hide the popup panel
                        hidePopupPanel();
                        leanbackOverlayFragment.hideOverlay();

                        // also close this if live tv
                        if (mPlaybackController.isLiveTv()) hide();
                        return true;
                    } else if (mGuideVisible) {
                        hideGuide();
                        return true;
                    }
                }

                if (mPlaybackController.isLiveTv() && !mPopupPanelVisible && !mGuideVisible && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!leanbackOverlayFragment.isControlsOverlayVisible()) {
                        leanbackOverlayFragment.setShouldShowOverlay(false);
                        leanbackOverlayFragment.hideOverlay();
                        showQuickChannelChanger();
                        return true;
                    }
                }

                if (mPopupPanelVisible && !mGuideVisible && keyCode == KeyEvent.KEYCODE_DPAD_LEFT && mPopupRowPresenter.getPosition() == 0) {
                    mPopupRowsFragment.getView().requestFocus();
                    mPopupRowPresenter.setPosition(0);
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
                        return true;
                    }
                }

                if (mPlaybackController.isLiveTv() && keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                    showGuide();
                    return true;
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

                    // Control fast forward and rewind if overlay hidden and not showing live TV
                    if (!mPlaybackController.isLiveTv()) {
                        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
                            mPlaybackController.fastForward();
                            if (!mIsVisible) show();
                            setFadingEnabled(true);
                            return true;
                        }

                        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                            mPlaybackController.rewind();
                            if (!mIsVisible) show();
                            setFadingEnabled(true);
                            return true;
                        }
                    }

                    if (!mIsVisible) {
                        if (!DeviceUtils.isFireTv() && !mPlaybackController.isLiveTv()) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                Utils.beep(100);
                                mPlaybackController.skip(30000);
                                mIsVisible = true;
                                setFadingEnabled(true);
                                return true;
                            }

                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                Utils.beep(100);
                                mPlaybackController.skip(-11000);
                                mIsVisible = true;
                                setFadingEnabled(true);
                                return true;
                            }
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
            }
            return false;
        }
    };

    private void switchChannelByNumber(String number) {
        List<ChannelInfoDto> channels = TvManager.getAllChannels();
        for (ChannelInfoDto channel:
                channels) {
            if (channel.getNumber().equals(number)) {
                switchChannel(channel.getId());
                break;
            }
        }
    }

    private void channelButton(boolean up) {
        int oldOffset = mChannelOffset;
        if (up) mChannelOffset += 1;
        else mChannelOffset -= 1;

        if (mAllChannels == null)
            mAllChannels = TvManager.getAllChannels();

        if (mAllChannels.size() > 0) {
            List<ChannelInfoDto> tempList = mAllChannels;
            if ("DatePlayed".equals(TvManager.getPrefs().channelOrder)) {
                Collections.sort(tempList, new Comparator<ChannelInfoDto>() {
                    @Override
                    public int compare(ChannelInfoDto channelOne, ChannelInfoDto channelTwo) {
                        return Integer.parseInt(channelOne.getNumber()) - Integer.parseInt(channelTwo.getNumber());
                    }
                });
            }

            String id = mPlaybackController.getCurrentlyPlayingItem().getId();

            if (id == null) {
                mChannelOffset = oldOffset;
                return;
            }

            int curChannelNdx = -1;
            for (int i = 0; i < tempList.size(); i++) {
                if (tempList.get(i).getId().equals(id)) {
                    curChannelNdx = i;
                    break;
                }
            }

            if (curChannelNdx + mChannelOffset < 0 || curChannelNdx + mChannelOffset > (tempList.size() - 1) || curChannelNdx == -1) {
                mChannelOffset = oldOffset;
                return;
            }

            mChannelNumberVisible = true;
            mChannelNumber = tempList.get(curChannelNdx + mChannelOffset).getNumber();
            mChannelNumberView.setVisibility(View.VISIBLE);
            mChannelNumberTextView.setText(mChannelNumber);

            mHandler.removeCallbacks(mHideNumberTask);
            mHandler.postDelayed(mHideNumberTask, 5000);
        }
    }

    private void hideChannelNumberView() {
        mChannelNumber = "";
        mChannelOffset = 0;
        mChannelNumberVisible = false;
        mChannelNumberView.setVisibility(View.GONE);

        leanbackOverlayFragment.setShouldShowOverlay(true);
    }

    public long getCurrentLocalStartDate() {
        return mCurrentLocalGuideStart;
    }

    public long getCurrentLocalEndDate() {
        return mCurrentLocalGuideEnd;
    }

    public void switchChannel(String id) {
        switchChannel(id, true);
    }

    public void switchChannel(String id, boolean hideGuide) {
        if (id == null) return;
        if (mPlaybackController.getCurrentlyPlayingItem().getId().equals(id)) {
            // same channel, just dismiss overlay
            if (hideGuide)
                hideGuide();
        } else {
            mPlaybackController.stop();
            if (hideGuide)
                hideGuide();
            apiClient.getValue().GetItemAsync(id, mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
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
            Timber.e("Unable to get audio focus");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        // register a media button receiver so that all media button presses will come to us and not another app
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(TvApp.getApplication().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+

        if (!mIsVisible) {
            show(); // in case we were paused during video playback
            setFadingEnabled(true);
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
        Timber.d("Fragment pausing. IsFinishing: %b", mActivity.isFinishing());
        if (!mActivity.isFinishing()) mActivity.finish(); // user hit "home" we want to back out
    }

    public void show() {
        hideInfo();
        mTopPanel.startAnimation(slideDown);
        mIsVisible = true;
    }

    public void hide() {
        mIsVisible = false;
        mTopPanel.startAnimation(fadeOut);
    }

    private void showChapterPanel() {
        setFadingEnabled(false);

        mPopupArea.startAnimation(showPopup);
    }

    private void hidePopupPanel() {
        setFadingEnabled(true);
        mPopupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
    }

    private void showInfo() {
        setFadingEnabled(false);
    }

    private void hideInfo() {
    }

    public void showGuide() {
        hide();
        leanbackOverlayFragment.setShouldShowOverlay(false);
        leanbackOverlayFragment.hideOverlay();
        mPlaybackController.mVideoManager.contractVideo(Utils.convertDpToPixel(mActivity, 300));
        mTvGuide.setVisibility(View.VISIBLE);
        mGuideVisible = true;
        Calendar now = Calendar.getInstance();
        boolean needLoad = mCurrentGuideStart == null;
        if (!needLoad) {
            Calendar needLoadTime = (Calendar) mCurrentGuideStart.clone();
            needLoadTime.add(Calendar.MINUTE, 30);
            needLoad = now.after(needLoadTime);
            if (mSelectedProgramView != null)
                mSelectedProgramView.requestFocus();
        }
        if (needLoad) {
            loadGuide();
        }
    }

    private void hideGuide() {
        mTvGuide.setVisibility(View.GONE);
        mPlaybackController.mVideoManager.setVideoFullSize(true);
        mGuideVisible = false;
    }

    private void loadGuide() {
        mGuideSpinner.setVisibility(View.VISIBLE);
        fillTimeLine(mGuideHours);
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

    public void displayChannels(int start, int max) {
        int end = start + max;
        if (end > mAllChannels.size()) end = mAllChannels.size();

        mCurrentDisplayChannelStartNdx = start;
        mCurrentDisplayChannelEndNdx = end - 1;
        Timber.d("*** Display channels pre-execute");
        mGuideSpinner.setVisibility(View.VISIBLE);

        mChannels.removeAllViews();
        mProgramRows.removeAllViews();
        mChannelStatus.setText("");
        mFilterStatus.setText("");
        TvManager.getProgramsAsync(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyResponse() {
            @Override
            public void onResponse() {
                Timber.d("*** Programs response");
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
            Timber.d("*** Display programs pre-execute");
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

                mProgramRows.addView(new GuidePagingButton(mActivity, mFragment, pageUpStart, getString(R.string.lbl_load_channels) + mAllChannels.get(pageUpStart).getNumber() + " - " + mAllChannels.get(mCurrentDisplayChannelStartNdx - 1).getNumber()));
            }
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int start = params[0];
            int end = params[1];

            boolean first = true;

            Timber.d("*** About to iterate programs");
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
                        GuideChannelHeader header = getChannelHeader(mActivity, channel, true);
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
            Timber.d("*** Display programs post execute");
            if (mCurrentDisplayChannelEndNdx < mAllChannels.size() - 1) {
                // Show a paging row for channels below
                int pageDnEnd = mCurrentDisplayChannelEndNdx + PAGE_SIZE;
                if (pageDnEnd >= mAllChannels.size()) pageDnEnd = mAllChannels.size() - 1;

                TextView placeHolder = new TextView(mActivity);
                placeHolder.setHeight(LiveTvGuideActivity.PAGEBUTTON_HEIGHT);
                mChannels.addView(placeHolder);

                mProgramRows.addView(new GuidePagingButton(mActivity, mFragment, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels) + mAllChannels.get(mCurrentDisplayChannelEndNdx + 1).getNumber() + " - " + mAllChannels.get(pageDnEnd).getNumber()));
            }

            mChannelStatus.setText(getResources().getString(R.string.lbl_tv_channel_status, displayedChannels, mAllChannels.size()));
            mFilterStatus.setText(getResources().getString(R.string.lbl_tv_filter_status, mGuideHours));
            mFilterStatus.setTextColor(Color.GRAY);

            if (firstRow != null) firstRow.requestFocus();
        }
    }

    private int currentCellId = 0;

    private GuideChannelHeader getChannelHeader(Context context, ChannelInfoDto channel, boolean focusable){
        return new GuideChannelHeader(context, this, channel, focusable);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {
        LinearLayout programRow = new LinearLayout(mActivity);
        if (programs.size() == 0) {

            int minutes = ((Long)((mCurrentLocalGuideEnd - mCurrentLocalGuideStart) / 60000)).intValue();
            int slot = 0;

            do {
                BaseItemDto empty = new BaseItemDto();
                empty.setName("  " + getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*slot) * 60000))));
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*(slot+1)) * 60000))));
                ProgramGridCell cell = new ProgramGridCell(mActivity, this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(30 * PIXELS_PER_MINUTE, LiveTvGuideActivity.ROW_HEIGHT));
                cell.setFocusable(true);
                programRow.addView(cell);
                if (slot == 0)
                    cell.setFirst();
                if (slot == (minutes / 30) - 1)
                    cell.setLast();
                slot++;
            } while((30*slot) < minutes);

            return programRow;
        }

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? TimeUtils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) start = getCurrentLocalStartDate();
            if (start > getCurrentLocalEndDate()) continue;
            if (start < prevEnd) continue;

            if (start > prevEnd) {
                // fill empty time slot
                BaseItemDto empty = new BaseItemDto();
                empty.setName("  " + getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
                Long duration = (start - prevEnd);
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd + duration)));
                ProgramGridCell cell = new ProgramGridCell(mActivity, mFragment, empty, false);
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
                ProgramGridCell program = new ProgramGridCell(mActivity, mFragment, item, false);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * PIXELS_PER_MINUTE, LiveTvGuideActivity.ROW_HEIGHT));
                program.setFocusable(true);

                if (start == getCurrentLocalStartDate())
                    program.setFirst();
                if (end == getCurrentLocalEndDate())
                    program.setLast();

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
                apiClient.getValue().GetItemAsync(mSelectedProgram.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        mSelectedProgram = response;
                        detailUpdateInternal();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Timber.e(exception, "Unable to get program details");
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
        //info row
        InfoLayoutHelper.addInfoRow(mActivity, mSelectedProgram, mGuideInfoRow, false, false);
        if (mSelectedProgram.getId() != null) {
            mDisplayDate.setText(TimeUtils.getFriendlyDate(TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));
        }

    }

    public void setSelectedProgram(RelativeLayout programView) {
        if (programView instanceof ProgramGridCell) {
            ProgramGridCell newView = (ProgramGridCell) programView;
            mSelectedProgramView = newView;
            mSelectedProgram = newView.getProgram();
            mHandler.removeCallbacks(detailUpdateTask);
            mHandler.postDelayed(detailUpdateTask, 500);
        }
    }

    public void dismissProgramOptions() {
        if (mDetailPopup != null) mDetailPopup.dismiss();
    }

    private LiveProgramDetailPopup mDetailPopup;

    public void showProgramOptions() {
        if (mSelectedProgram == null) return;
        if (mDetailPopup == null)
            mDetailPopup = new LiveProgramDetailPopup(mActivity, Utils.convertDpToPixel(mActivity, 600), new EmptyResponse() {
                @Override
                public void onResponse() {
                    switchChannel(mSelectedProgram.getChannelId());
                }
            });
        mDetailPopup.setContent(mSelectedProgram, mSelectedProgramView);
        mDetailPopup.show(mGuideTitle, 0, mGuideTitle.getTop() - 10);

    }

    private Animation.AnimationListener hideAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mTopPanel.setVisibility(View.GONE);
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
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void updatePoster(BaseItemDto item, ImageView target, boolean preferSeries) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int height = Utils.convertDpToPixel(getActivity(), 300);
            int width = Utils.convertDpToPixel(getActivity(), 150);
            String posterImageUrl = ImageUtils.getPrimaryImageUrl(item, apiClient.getValue(), false, false, preferSeries, height);
            if (posterImageUrl != null)
                Glide.with(getActivity()).load(posterImageUrl).override(width, height).centerInside().into(target);
        }
    }

    private void updateLogo(BaseItemDto item, ImageView target) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            int height = Utils.convertDpToPixel(getActivity(), 60);
            int width = Utils.convertDpToPixel(getActivity(), 180);
            String imageUrl = ImageUtils.getLogoImageUrl(item, apiClient.getValue());
            if (imageUrl != null)
                Glide.with(getActivity()).load(imageUrl).override(width, height).centerInside().into(target);
        }
    }

    public void showQuickChannelChanger() {
        showChapterPanel();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ndx = TvManager.getAllChannelsIndex(TvManager.getLastLiveTvChannel());
                if (ndx > 0) {
                    mPopupRowPresenter.setPosition(ndx);
                }
                mPopupPanelVisible = true;
            }
        }, 500);
    }

    public void showChapterSelector() {
        showChapterPanel();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int ndx = getCurrentChapterIndex(mPlaybackController.getCurrentlyPlayingItem(), mPlaybackController.getCurrentPosition() * 10000);
                if (ndx > 0) {
                    mPopupRowPresenter.setPosition(ndx);
                }
                mPopupPanelVisible = true;
            }
        }, 500);
    }

    private int getCurrentChapterIndex(BaseItemDto item, long pos) {
        int ndx = 0;
        Timber.d("*** looking for chapter at pos: %d", pos);
        if (item.getChapters() != null) {
            for (ChapterInfoDto chapter : item.getChapters()) {
                Timber.d("*** chapter %d has pos: %d", ndx, chapter.getStartPositionTicks());
                if (chapter.getStartPositionTicks() > pos) return ndx - 1;
                ndx++;
            }
        }
        return ndx - 1;
    }

    public void toggleRecording(BaseItemDto item) {
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
                            .setNegativeButton(R.string.just_one, new DialogInterface.OnClickListener() {
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
                apiClient.getValue().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyResponse() {
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
                apiClient.getValue().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
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
            apiClient.getValue().GetDefaultLiveTvTimerInfo(new Response<SeriesTimerInfoDto>() {
                @Override
                public void onResponse(SeriesTimerInfoDto response) {
                    response.setProgramId(program.getId());
                    if (series) {
                        apiClient.getValue().CreateLiveTvSeriesTimerAsync(response, new EmptyResponse() {
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
                        apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyResponse() {
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
        leanbackOverlayFragment.updateCurrentPosition();
    }

    @Override
    public void setSecondaryTime(long time) {
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
        leanbackOverlayFragment.updatePlayState();
    }

    @Override
    public void updateDisplay() {
        BaseItemDto current = mPlaybackController.getCurrentlyPlayingItem();
        if (current != null & mActivity != null && !mActivity.isFinishing()) {
            leanbackOverlayFragment.mediaInfoChanged();
            leanbackOverlayFragment.onFullyInitialized();
            leanbackOverlayFragment.recordingStateChanged();
            // set progress to match duration
            // set other information
            mGuideCurrentTitle.setText(current.getName());
            updateLogo(current, mLogoImage);
            if (mPlaybackController.isLiveTv()) {
                prepareChannelAdapter();
            } else {
                prepareChapterAdapter();
            }
        }
    }

    private void prepareChapterAdapter() {
        BaseItemDto item = mPlaybackController.getCurrentlyPlayingItem();
        List<ChapterInfoDto> chapters = item.getChapters();

        if (chapters != null && !chapters.isEmpty()) {
            // create chapter row for later use
            ItemRowAdapter chapterAdapter = new ItemRowAdapter(BaseItemUtils.buildChapterItems(item), new CardPresenter(true, 220), new ArrayObjectAdapter());
            chapterAdapter.Retrieve();
            if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
            mChapterRow = new ListRow(new HeaderItem(mActivity.getString(R.string.chapters)), chapterAdapter);
            mPopupRowAdapter.add(mChapterRow);
        }

    }

    private void prepareChannelAdapter() {
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
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    private SubtitleTrackInfo mManualSubs;
    private long lastReportedPosMs;

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

    @Override
    public void showNextUp(String id) {
        // Set to "modified" so the queue won't be cleared
        MediaManager.setVideoQueueModified(true);

        Intent intent = new Intent(getActivity(), NextUpActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
        finish();
    }

    public void updateSubtitles(long positionMs) {
        if (lastReportedPosMs > 0) {
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

                // Encode whitespace as html entities
                text = text.replaceAll("\\r\\n", "<br>");
                text = text.replaceAll("\\\\h", "&ensp;");

                SpannableString span = new SpannableString(TextUtilsKt.toHtmlSpanned(text));
                span.setSpan(new ForegroundColorSpan(Color.WHITE), 0, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                span.setSpan(new BackgroundColorSpan(ContextCompat.getColor(mActivity, R.color.black_opaque)), 0, span.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                mSubtitleText.setText(span);
                mSubtitleText.setVisibility(View.VISIBLE);
            }
        });
    }
}
