package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.databinding.OverlayTvGuideBinding;
import org.jellyfin.androidtv.databinding.VlcPlayerInterfaceBinding;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.ui.GuideChannelHeader;
import org.jellyfin.androidtv.ui.GuidePagingButton;
import org.jellyfin.androidtv.ui.HorizontalScrollViewListener;
import org.jellyfin.androidtv.ui.LiveProgramDetailPopup;
import org.jellyfin.androidtv.ui.ObservableHorizontalScrollView;
import org.jellyfin.androidtv.ui.ObservableScrollView;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.ScrollViewListener;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.playback.nextup.NextUpActivity;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.ChannelCardPresenter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.PaddedLineBackgroundSpan;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.ChapterInfoDto;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackEvent;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class CustomPlaybackOverlayFragment extends Fragment implements LiveTvGuide {
    private VlcPlayerInterfaceBinding binding;
    private OverlayTvGuideBinding tvGuideBinding;

    private RowsSupportFragment mPopupRowsFragment;
    private ListRow mChapterRow;
    private ArrayObjectAdapter mPopupRowAdapter;
    private PositionableListRowPresenter mPopupRowPresenter;

    //Live guide items
    private static final int PAGE_SIZE = 75;
    private static final int GUIDE_HOURS = 9;

    private BaseItemDto mSelectedProgram;
    private RelativeLayout mSelectedProgramView;
    private boolean mGuideVisible = false;
    private Calendar mCurrentGuideStart;
    private Calendar mCurrentGuideEnd;
    private long mCurrentLocalGuideStart;
    private long mCurrentLocalGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;
    private List<ChannelInfoDto> mAllChannels;
    private String mFirstFocusChannelId;

    private PlaybackController mPlaybackController;
    private List<BaseItemDto> mItemsToPlay;

    private Animation fadeOut;
    private Animation slideUp;
    private Animation slideDown;
    private Animation showPopup;
    private Animation hidePopup;
    private final Handler mHandler = new Handler();
    private Runnable mHideTask;

    private AudioManager mAudioManager;

    private boolean mFadeEnabled = false;
    private boolean mIsVisible = false;
    private boolean mPopupPanelVisible = false;

    private LeanbackOverlayFragment leanbackOverlayFragment;

    // Subtitle fields
    private static final int SUBTITLE_PADDING = 8;
    private static final long SUBTITLE_RENDER_INTERVAL_MS = 50;
    private SubtitleTrackInfo subtitleTrackInfo;
    private int currentSubtitleIndex = 0;
    private int subtitlesSize = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDefaultSubtitlesSize());
    private long lastSubtitlePositionMs = 0;
    private boolean subtitlesBackgroundEnabled = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getSubtitlesBackgroundEnabled());

    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<PlaybackControllerContainer> playbackControllerContainer = inject(PlaybackControllerContainer.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // stop any audio that may be playing
        mediaManager.getValue().stopAudio(true);

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Timber.e("Unable to get audio manager");
            Utils.showToast(getActivity(), R.string.msg_cannot_play_time);
            return;
        }

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        ((PlaybackOverlayActivity) requireActivity()).setKeyListener(keyListener);

        mItemsToPlay = mediaManager.getValue().getCurrentVideoQueue();
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(getContext(), getString(R.string.msg_no_playable_items));
            requireActivity().finish();
            return;
        }

        playbackControllerContainer.getValue().setPlaybackController(new PlaybackController(mItemsToPlay, this));
        mPlaybackController = playbackControllerContainer.getValue().getPlaybackController();

        // setup fade task
        mHideTask = () -> {
            if (mIsVisible) {
                hide();
                leanbackOverlayFragment.hideOverlay();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = VlcPlayerInterfaceBinding.inflate(inflater, container, false);

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
        tvGuideBinding = OverlayTvGuideBinding.inflate(inflater, container, false);
        binding.getRoot().addView(tvGuideBinding.getRoot());
        tvGuideBinding.getRoot().setVisibility(View.GONE);

        binding.getRoot().setOnTouchListener((v, event) -> {
            //if we're not visible, show us
            if (!mIsVisible) show();

            //and then manage our fade timer
            if (mFadeEnabled) startFadeTimer();

            Timber.d("Got touch event.");
            return false;
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (playbackControllerContainer.getValue().getPlaybackController() != null) {
            playbackControllerContainer.getValue().getPlaybackController().init(new VideoManager(((PlaybackOverlayActivity) requireActivity()), view), this);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) return;

        prepareOverlayFragment();

        //manual subtitles
        // This configuration is required for the PaddedLineBackgroundSpan to work
        binding.subtitlesText.setShadowLayer(SUBTITLE_PADDING, 0, 0, Color.TRANSPARENT);
        binding.subtitlesText.setPadding(SUBTITLE_PADDING, 0, SUBTITLE_PADDING, 0);

        // Subtitles font size configuration
        binding.subtitlesText.setTextSize(subtitlesSize);

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.abc_slide_in_bottom);
        slideDown.setAnimationListener(showAnimationListener);
        setupPopupAnimations();

        //live guide
        tvGuideBinding.channelsStatus.setTextColor(Color.GRAY);
        tvGuideBinding.filterStatus.setTextColor(Color.GRAY);

        tvGuideBinding.programRows.setFocusable(false);
        tvGuideBinding.programVScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                tvGuideBinding.channelScroller.scrollTo(x, y);
            }
        });

        tvGuideBinding.channelScroller.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                tvGuideBinding.programVScroller.scrollTo(x, y);
            }
        });

        tvGuideBinding.timelineHScroller.setFocusable(false);
        tvGuideBinding.timelineHScroller.setFocusableInTouchMode(false);
        tvGuideBinding.timeline.setFocusable(false);
        tvGuideBinding.timeline.setFocusableInTouchMode(false);
        tvGuideBinding.channelScroller.setFocusable(false);
        tvGuideBinding.channelScroller.setFocusableInTouchMode(false);

        tvGuideBinding.programHScroller.setScrollViewListener(new HorizontalScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableHorizontalScrollView scrollView, int x, int y, int oldx, int oldy) {
                tvGuideBinding.timelineHScroller.scrollTo(x, y);
            }
        });
        tvGuideBinding.programHScroller.setFocusable(false);
        tvGuideBinding.programHScroller.setFocusableInTouchMode(false);

        tvGuideBinding.channels.setFocusable(false);
        tvGuideBinding.channelScroller.setFocusable(false);

        // register to receive message from popup
        ((PlaybackOverlayActivity) requireActivity()).registerMessageListener(message -> {
            if (message.equals(CustomMessage.ActionComplete)) dismissProgramOptions();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(backPressedCallback);


        Intent intent = requireActivity().getIntent();
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
        showPopup = AnimationUtils.loadAnimation(requireContext(), R.anim.abc_slide_in_bottom);
        showPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                binding.popupArea.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.popupArea.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        hidePopup = AnimationUtils.loadAnimation(requireContext(), R.anim.abc_fade_out);
        hidePopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.popupArea.setVisibility(View.GONE);
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
            } else if (!requireActivity().isFinishing()) {
                requireActivity().finish();
            }
        }
    };

    public boolean onKeyUp(int keyCode, KeyEvent event){
        if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
            if (mGuideVisible && mSelectedProgramView instanceof ProgramGridCell && mSelectedProgram != null && mSelectedProgram.getChannelId() != null) {
                Date curUTC = TimeUtils.convertToUtcDate(new Date());
                if (mSelectedProgram.getStartDate().before(curUTC))
                    switchChannel(mSelectedProgram.getChannelId());
                else
                    showProgramOptions();
                return true;
            }else if (mSelectedProgramView instanceof GuideChannelHeader) {
                switchChannel(((GuideChannelHeader)mSelectedProgramView).getChannel().getId(), false);
            }
        }
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event){
        if (mSelectedProgramView instanceof ProgramGridCell)
            showProgramOptions();
        else if(mSelectedProgramView instanceof GuideChannelHeader)
            toggleFavorite();
        return true;
    }

    public void refreshFavorite(String channelId){
        for (int i = 0; i < tvGuideBinding.channels.getChildCount(); i++) {
            GuideChannelHeader gch = (GuideChannelHeader) tvGuideBinding.channels.getChildAt(i);
            if (gch.getChannel().getId().equals(channelId))
                gch.refreshFavorite();
        }
    }

    private void toggleFavorite() {
        GuideChannelHeader header = (GuideChannelHeader)mSelectedProgramView;
        UserItemDataDto data = header.getChannel().getUserData();
        if (data != null) {
            apiClient.getValue().UpdateFavoriteStatusAsync(header.getChannel().getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                @Override
                public void onResponse(UserItemDataDto response) {
                    header.getChannel().setUserData(response);
                    header.findViewById(R.id.favImage).setVisibility(response.getIsFavorite() ? View.VISIBLE : View.GONE);
                    DataRefreshService dataRefreshService = KoinJavaComponent.<DataRefreshService>get(DataRefreshService.class);
                    dataRefreshService.setLastFavoriteUpdate(System.currentTimeMillis());
                }
            });
        }
    }

    private View.OnKeyListener keyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mGuideVisible)
                    leanbackOverlayFragment.setShouldShowOverlay(true);
                else {
                    leanbackOverlayFragment.setShouldShowOverlay(false);
                    leanbackOverlayFragment.hideOverlay();
                }

                if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP && !requireActivity().isFinishing()) {
                    requireActivity().finish();
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
                    mPopupRowsFragment.requireView().requestFocus();
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
                            setFadingEnabled(true);
                            return true;
                        }

                        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                            mPlaybackController.rewind();
                            setFadingEnabled(true);
                            return true;
                        }
                    }

                    if (!mIsVisible) {
                        if (!mPlaybackController.isLiveTv()) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                setFadingEnabled(true);
                                return true;
                            }

                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                setFadingEnabled(true);
                                return true;
                            }
                        }

                        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && mPlaybackController.canSeek()) {
                            // if the player is playing and the overlay is hidden, this will pause
                            // if the player is paused and then 'back' is pressed to hide the overlay, this will play
                            mPlaybackController.playPause();
                            return true;
                        }

                        //if we're not visible, show us
                        show();
                    }

                    //and then manage our fade timer
                    if (mFadeEnabled) startFadeTimer();
                }
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    leanbackOverlayFragment.getPlayerGlue().setInjectedViewsVisibility();
            }

            return false;
        }
    };

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
        if (Utils.isEmpty(id)) return;
        if (mPlaybackController.getCurrentlyPlayingItem().getId().equals(id)) {
            // same channel, just dismiss overlay
            if (hideGuide)
                hideGuide();
        } else {
            mPlaybackController.stop();
            if (hideGuide)
                hideGuide();
            apiClient.getValue().GetItemAsync(id, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    List<BaseItemDto> items = new ArrayList<BaseItemDto>();
                    items.add(response);
                    mPlaybackController.setItems(items);
                    mPlaybackController.play(0);
                }

                @Override
                public void onError(Exception exception) {
                    Utils.showToast(getContext(), R.string.msg_video_playback_error);
                    finish();
                }
            });
        }
    }

    private void startFadeTimer() {
        mFadeEnabled = true;
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
    }

    @Override
    public void onPause() {
        super.onPause();

        setPlayPauseActionState(0);

        // give back audio focus
        mAudioManager.abandonAudioFocus(mAudioFocusChanged);
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("Stopping!");

        if (leanbackOverlayFragment != null)
            leanbackOverlayFragment.setOnKeyInterceptListener(null);
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }

        ((PlaybackOverlayActivity) requireActivity()).removeMessageListener();
        ((PlaybackOverlayActivity) requireActivity()).setKeyListener(null);

        // end playback from here if this fragment belongs to the current session.
        // if it doesn't, playback has already been stopped elsewhere, and the references to this have been replaced
        if (mPlaybackController != null && mPlaybackController.getFragment() == this) {
            Timber.d("this fragment belongs to the current session, ending it");
            mPlaybackController.endPlayback();
        }

        if (!requireActivity().isFinishing()) {
            // in case the app is suspended/stopped, eg: by pressing the home button, end the playback session.
            requireActivity().finish();
        }
    }

    public void show() {
        binding.topPanel.startAnimation(slideDown);
        mIsVisible = true;
    }

    public void hide() {
        mIsVisible = false;
        binding.topPanel.startAnimation(fadeOut);
    }

    private void showChapterPanel() {
        setFadingEnabled(false);
        binding.popupArea.startAnimation(showPopup);
    }

    private void hidePopupPanel() {
        startFadeTimer();
        binding.popupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
    }

    public void showGuide() {
        hide();
        leanbackOverlayFragment.setShouldShowOverlay(false);
        leanbackOverlayFragment.hideOverlay();
        mPlaybackController.mVideoManager.contractVideo(Utils.convertDpToPixel(requireContext(), 300));
        tvGuideBinding.getRoot().setVisibility(View.VISIBLE);
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
        tvGuideBinding.getRoot().setVisibility(View.GONE);
        mPlaybackController.mVideoManager.setVideoFullSize(true);
        mGuideVisible = false;
    }

    private void loadGuide() {
        tvGuideBinding.spinner.setVisibility(View.VISIBLE);
        fillTimeLine(GUIDE_HOURS);
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
                    tvGuideBinding.spinner.setVisibility(View.GONE);
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
        tvGuideBinding.spinner.setVisibility(View.VISIBLE);

        tvGuideBinding.channels.removeAllViews();
        tvGuideBinding.programRows.removeAllViews();
        tvGuideBinding.channelsStatus.setText("");
        tvGuideBinding.filterStatus.setText("");
        final CustomPlaybackOverlayFragment self = this;
        TvManager.getProgramsAsync(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyResponse() {
            @Override
            public void onResponse() {
                Timber.d("*** Programs response");
                if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
                mDisplayProgramsTask = new DisplayProgramsTask(self);
                mDisplayProgramsTask.execute(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx);
            }
        });
    }

    DisplayProgramsTask mDisplayProgramsTask;

    class DisplayProgramsTask extends AsyncTask<Integer, Integer, Void> {
        private View firstRow;
        private int displayedChannels = 0;
        private final LiveTvGuide guide;

        DisplayProgramsTask(LiveTvGuide guide) {
            super();
            this.guide = guide;
        }

        @Override
        protected void onPreExecute() {
            Timber.d("*** Display programs pre-execute");
            tvGuideBinding.channels.removeAllViews();
            tvGuideBinding.programRows.removeAllViews();
            mFirstFocusChannelId = mPlaybackController.getCurrentlyPlayingItem().getId();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) pageUpStart = 0;

                TextView placeHolder = new TextView(requireContext());
                placeHolder.setHeight(Utils.convertDpToPixel(getContext(), 20));
                tvGuideBinding.channels.addView(placeHolder);
                displayedChannels = 0;

                tvGuideBinding.programRows.addView(new GuidePagingButton(requireActivity(), guide, pageUpStart, getString(R.string.lbl_load_channels) + mAllChannels.get(pageUpStart).getNumber() + " - " + mAllChannels.get(mCurrentDisplayChannelStartNdx - 1).getNumber()));
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

                requireActivity().runOnUiThread(() -> {
                    GuideChannelHeader header = getChannelHeader(requireContext(), channel);
                    tvGuideBinding.channels.addView(header);
                    header.loadImage();
                    tvGuideBinding.programRows.addView(row);
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

                TextView placeHolder = new TextView(requireContext());
                placeHolder.setHeight(Utils.convertDpToPixel(getContext(), 20));
                tvGuideBinding.channels.addView(placeHolder);

                tvGuideBinding.programRows.addView(new GuidePagingButton(requireActivity(), guide, mCurrentDisplayChannelEndNdx + 1, getString(R.string.lbl_load_channels) + mAllChannels.get(mCurrentDisplayChannelEndNdx + 1).getNumber() + " - " + mAllChannels.get(pageDnEnd).getNumber()));
            }

            tvGuideBinding.channelsStatus.setText(getResources().getString(R.string.lbl_tv_channel_status, displayedChannels, mAllChannels.size()));
            tvGuideBinding.filterStatus.setText(getResources().getString(R.string.lbl_tv_filter_status, GUIDE_HOURS));

            tvGuideBinding.spinner.setVisibility(View.GONE);

            if (firstRow != null) firstRow.requestFocus();
        }
    }

    private int currentCellId = 0;

    private GuideChannelHeader getChannelHeader(Context context, ChannelInfoDto channel){
        return new GuideChannelHeader(context, this, channel);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {
        LinearLayout programRow = new LinearLayout(requireContext());
        if (programs.size() == 0) {

            int minutes = ((Long)((mCurrentLocalGuideEnd - mCurrentLocalGuideStart) / 60000)).intValue();
            int slot = 0;

            do {
                BaseItemDto empty = new BaseItemDto();
                empty.setName("  " + getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*slot) * 60000))));
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30*(slot+1)) * 60000))));
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(30 * Utils.convertDpToPixel(getContext(), 7), Utils.convertDpToPixel(getContext(), 55)));
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
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long) (duration / 60000)).intValue() * Utils.convertDpToPixel(getContext(), 7), Utils.convertDpToPixel(getContext(), 55)));
                cell.setFocusable(true);
                programRow.addView(cell);
            }
            long end = item.getEndDate() != null ? TimeUtils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end - start) / 60000;
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(requireContext(), this, item, false);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * Utils.convertDpToPixel(getContext(), 7), Utils.convertDpToPixel(getContext(), 55)));
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

        tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mCurrentGuideStart.getTime()));
        Calendar current = (Calendar) mCurrentGuideStart.clone();
        mCurrentGuideEnd = (Calendar) mCurrentGuideStart.clone();
        int oneHour = 60 * Utils.convertDpToPixel(getContext(), 7);
        int halfHour = 30 * Utils.convertDpToPixel(getContext(), 7);
        int interval = current.get(Calendar.MINUTE) >= 30 ? 30 : 60;
        mCurrentGuideEnd.add(Calendar.HOUR, hours);
        mCurrentLocalGuideEnd = mCurrentGuideEnd.getTimeInMillis();
        tvGuideBinding.timeline.removeAllViews();
        while (current.before(mCurrentGuideEnd)) {
            TextView time = new TextView(requireContext());
            time.setText(android.text.format.DateFormat.getTimeFormat(requireContext()).format(current.getTime()));
            time.setWidth(interval == 30 ? halfHour : oneHour);
            tvGuideBinding.timeline.addView(time);
            current.add(Calendar.MINUTE, interval);
            //after first one, we always go on hours
            interval = 60;
        }
    }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (mSelectedProgram.getOverview() == null && mSelectedProgram.getId() != null) {
                apiClient.getValue().GetItemAsync(mSelectedProgram.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new Response<BaseItemDto>() {
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
        tvGuideBinding.guideTitle.setText(mSelectedProgram.getName());
        tvGuideBinding.summary.setText(mSelectedProgram.getOverview());
        //info row
        InfoLayoutHelper.addInfoRow(requireContext(), mSelectedProgram, tvGuideBinding.guideInfoRow, false, false);
        if (mSelectedProgram.getId() != null) {
            tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));
        }

    }

    public void setSelectedProgram(RelativeLayout programView) {
        mSelectedProgramView = programView;
        if (mSelectedProgramView instanceof ProgramGridCell) {
            mSelectedProgram = ((ProgramGridCell)mSelectedProgramView).getProgram();
            mHandler.removeCallbacks(detailUpdateTask);
            mHandler.postDelayed(detailUpdateTask, 500);
        } else if (mSelectedProgramView instanceof GuideChannelHeader) {
            for (int i = 0; i < tvGuideBinding.channels.getChildCount(); i++) {
                if (mSelectedProgramView == tvGuideBinding.channels.getChildAt(i)) {
                    LinearLayout programRow = (LinearLayout) tvGuideBinding.programRows.getChildAt(i);
                    if (programRow == null)
                        return;
                    Date utcTime = TimeUtils.convertToUtcDate(new Date());
                    for (int ii = 0; ii < programRow.getChildCount(); ii++) {
                        ProgramGridCell prog = (ProgramGridCell)programRow.getChildAt(ii);
                        if (prog.getProgram() != null && prog.getProgram().getStartDate().before(utcTime) && prog.getProgram().getEndDate().after(utcTime)) {
                            mSelectedProgram = prog.getProgram();
                            if (mSelectedProgram != null) {
                                mHandler.removeCallbacks(detailUpdateTask);
                                mHandler.postDelayed(detailUpdateTask, 500);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    public void dismissProgramOptions() {
        if (mDetailPopup != null) mDetailPopup.dismiss();
    }

    private LiveProgramDetailPopup mDetailPopup;

    public void showProgramOptions() {
        if (mSelectedProgram == null) return;
        if (mDetailPopup == null)
            mDetailPopup = new LiveProgramDetailPopup(((PlaybackOverlayActivity) requireActivity()), this, Utils.convertDpToPixel(requireContext(), 600), new EmptyResponse() {
                @Override
                public void onResponse() {
                    switchChannel(mSelectedProgram.getChannelId());
                }
            });
        mDetailPopup.setContent(mSelectedProgram, (ProgramGridCell)mSelectedProgramView);
        mDetailPopup.show(tvGuideBinding.guideTitle, 0, tvGuideBinding.guideTitle.getTop() - 10);

    }

    private Animation.AnimationListener hideAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            binding.topPanel.setVisibility(View.GONE);
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
            binding.topPanel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    public void showQuickChannelChanger() {
        showChapterPanel();
        mHandler.postDelayed(() -> {
            int ndx = TvManager.getAllChannelsIndex(TvManager.getLastLiveTvChannel());
            if (ndx > 0) {
                mPopupRowPresenter.setPosition(ndx);
            }
            mPopupPanelVisible = true;
        }, 500);
    }

    public void showChapterSelector() {
        showChapterPanel();
        mHandler.postDelayed(() -> {
            int ndx = getCurrentChapterIndex(mPlaybackController.getCurrentlyPlayingItem(), mPlaybackController.getCurrentPosition() * 10000);
            if (ndx > 0) {
                mPopupRowPresenter.setPosition(ndx);
            }
            mPopupPanelVisible = true;
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
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lbl_cancel_recording)
                            .setMessage(R.string.msg_cancel_entire_series)
                            .setPositiveButton(R.string.lbl_cancel_series, (dialog, which) -> cancelRecording(program, true))
                            .setNegativeButton(R.string.just_one, (dialog, which) -> cancelRecording(program, false))
                            .show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lbl_cancel_recording)
                            .setPositiveButton(R.string.lbl_yes, (dialog, which) -> cancelRecording(program, false))
                            .setNegativeButton(R.string.lbl_no, null)
                            .show();
                }
            } else {
                if (Utils.isTrue(program.getIsSeries())) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lbl_record_series)
                            .setMessage(R.string.msg_record_entire_series)
                            .setPositiveButton(R.string.lbl_record_series, (dialog, which) -> recordProgram(program, true))
                            .setNegativeButton(R.string.lbl_just_this_once, (dialog, which) -> recordProgram(program, false))
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
                        Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                        mPlaybackController.updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
                    }
                });
            } else {
                apiClient.getValue().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyResponse() {
                    @Override
                    public void onResponse() {
                        Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                        mPlaybackController.updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
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
                                Utils.showToast(requireContext(), R.string.msg_set_to_record);
                                mPlaybackController.updateTvProgramInfo();
                                TvManager.forceReload();
                            }

                            @Override
                            public void onError(Exception ex) {
                                Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                            }
                        });
                    } else {
                        apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyResponse() {
                            @Override
                            public void onResponse() {
                                Utils.showToast(requireContext(), R.string.msg_set_to_record);
                                mPlaybackController.updateTvProgramInfo();
                                TvManager.forceReload();
                            }

                            @Override
                            public void onError(Exception ex) {
                                Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                            }
                        });
                    }
                }
            });
        }
    }

    public void setCurrentTime(long time) {
        if (leanbackOverlayFragment != null)
            leanbackOverlayFragment.updateCurrentPosition();
    }

    public void setSecondaryTime(long time) {
    }

    public void setFadingEnabled(boolean value) {
        mFadeEnabled = value;
        if (!mIsVisible) requireActivity().runOnUiThread(this::show);
        if (mFadeEnabled) {
            startFadeTimer();
        } else {
            mHandler.removeCallbacks(mHideTask);
        }
    }

    public void setPlayPauseActionState(final int state) {
        leanbackOverlayFragment.updatePlayState();
    }

    public void updateDisplay() {
        BaseItemDto current = mPlaybackController.getCurrentlyPlayingItem();
        if (current != null && getActivity() != null && !getActivity().isFinishing()) {
            leanbackOverlayFragment.mediaInfoChanged();
            leanbackOverlayFragment.onFullyInitialized();
            leanbackOverlayFragment.recordingStateChanged();
            // set progress to match duration
            // set other information
            tvGuideBinding.guideCurrentTitle.setText(current.getName());

            // Update the title and subtitle
            if (current.getBaseItemType() == BaseItemType.Episode) {
                binding.itemTitle.setText(current.getSeriesName());
                binding.itemSubtitle.setText(BaseItemUtils.getDisplayName(current, requireContext()));
            } else {
                binding.itemTitle.setText(current.getName());
            }
            // Update the logo
            String imageUrl = ImageUtils.getLogoImageUrl(current, 440, false);
            if (imageUrl != null) {
                binding.itemLogo.setVisibility(View.VISIBLE);
                binding.itemTitle.setVisibility(View.GONE);
                binding.itemLogo.setContentDescription(current.getName());
                binding.itemLogo.load(imageUrl, null, null, 1.0, 0);
            } else {
                binding.itemLogo.setVisibility(View.GONE);
                binding.itemTitle.setVisibility(View.VISIBLE);
            }

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
            ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), BaseItemUtils.buildChapterItems(item), new CardPresenter(true, 220), new ArrayObjectAdapter());
            chapterAdapter.Retrieve();
            if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
            mChapterRow = new ListRow(new HeaderItem(requireContext().getString(R.string.chapters)), chapterAdapter);
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

    public void finish() {
        if (!requireActivity().isFinishing()) {
            requireActivity().finish();
        }
    }

    public void showNextUp(String id) {
        Intent intent = new Intent(getActivity(), NextUpActivity.class);
        intent.putExtra(NextUpActivity.EXTRA_ID, id);
        startActivity(intent);
        finish();
    }

    public void addManualSubtitles(@Nullable SubtitleTrackInfo info) {
        subtitleTrackInfo = info;
        currentSubtitleIndex = -1;
        lastSubtitlePositionMs = 0;
        clearSubtitles();
    }

    public void showSubLoadingMsg(final boolean show) {
        if (show) {
            renderSubtitles(requireContext().getString(R.string.msg_subtitles_loading));
        } else {
            clearSubtitles();
        }
    }

    public void updateSubtitles(long positionMs) {
        int iterCount = 1;
        final long positionTicks = positionMs * 10000;
        final long lastPositionTicks = lastSubtitlePositionMs * 10000;

        if (subtitleTrackInfo == null
                || subtitleTrackInfo.getTrackEvents() == null
                || subtitleTrackInfo.getTrackEvents().size() < 1
                || currentSubtitleIndex >= subtitleTrackInfo.getTrackEvents().size()) {
            return;
        }

        if (positionTicks < subtitleTrackInfo.getTrackEvents().get(0).getStartPositionTicks())
            return;

        // Skip rendering if the interval ms have not passed since last render
        if (lastSubtitlePositionMs > 0
                && Math.abs(lastSubtitlePositionMs - positionMs) < SUBTITLE_RENDER_INTERVAL_MS) {
            return;
        }

        // If the user has skipped back, reset the subtitle index
        if (lastSubtitlePositionMs > positionMs) {
            currentSubtitleIndex = -1;
            clearSubtitles();
        }

        if (currentSubtitleIndex == -1)
            Timber.d("subtitle track events size %s", subtitleTrackInfo.getTrackEvents().size());

        // Find the next subtitle event that should be rendered
        for (int tmpSubtitleIndex = currentSubtitleIndex == -1 ? 0 : currentSubtitleIndex; tmpSubtitleIndex < subtitleTrackInfo.getTrackEvents().size(); tmpSubtitleIndex++) {
            SubtitleTrackEvent trackEvent = subtitleTrackInfo.getTrackEvents().get(tmpSubtitleIndex);

            if (positionTicks >= trackEvent.getStartPositionTicks()
                    && positionTicks < trackEvent.getEndPositionTicks()) {
                // This subtitle event should be displayed now
                // use lastPositionTicks to ensure it is only rendered once

                if (lastPositionTicks < trackEvent.getStartPositionTicks() || lastPositionTicks >= trackEvent.getEndPositionTicks()) {
                    Timber.d("rendering subtitle event: %s (pos %s start %s end %s)", tmpSubtitleIndex, positionMs, trackEvent.getStartPositionTicks() / 10000, trackEvent.getEndPositionTicks() / 10000);
                    renderSubtitles(trackEvent.getText());
                }

                currentSubtitleIndex = tmpSubtitleIndex;
                lastSubtitlePositionMs = positionMs;
                // rendering should happen on the 2nd iteration
                if (iterCount > 2)
                    Timber.d("subtitles handled in %s iterations", iterCount);
                return;
            } else if (tmpSubtitleIndex < subtitleTrackInfo.getTrackEvents().size() - 1) {
                SubtitleTrackEvent nextTrackEvent = subtitleTrackInfo.getTrackEvents().get(tmpSubtitleIndex + 1);

                if (positionTicks >= trackEvent.getEndPositionTicks() && positionTicks < nextTrackEvent.getStartPositionTicks()) {
                    // clear the subtitles if between events
                    // use lastPositionTicks to ensure it is only cleared once

                    if (currentSubtitleIndex > -1 && !(lastPositionTicks >= trackEvent.getEndPositionTicks() && lastPositionTicks < nextTrackEvent.getStartPositionTicks())) {
                        Timber.d("clearing subtitle event: %s (pos %s - event end %s)", tmpSubtitleIndex, positionMs, trackEvent.getEndPositionTicks() / 10000);
                        clearSubtitles();
                    }

                    // set currentSubtitleIndex in case it was -1
                    currentSubtitleIndex = tmpSubtitleIndex;
                    lastSubtitlePositionMs = positionMs;
                    if (iterCount > 1)
                        Timber.d("subtitles handled in %s iterations", iterCount);
                    return;
                }
            }
            iterCount++;
        }
        // handles clearing the last event
        if (iterCount > 1)
            Timber.d("subtitles handled in %s iterations", iterCount);
        clearSubtitles();
    }

    private void clearSubtitles() {
        requireActivity().runOnUiThread(() -> {
            binding.subtitlesText.setVisibility(View.INVISIBLE);
            binding.subtitlesText.setText(null);
        });
    }

    private void renderSubtitles(@Nullable final String text) {
        if (text == null || text.length() == 0) {
            clearSubtitles();
            return;
        }
        requireActivity().runOnUiThread(() -> {
            // Encode whitespace as html entities
            final String htmlText = text
                    .replaceAll("\\r\\n", "<br>")
                    .replaceAll("\\n", "<br>")
                    .replaceAll("\\\\h", "&ensp;");

            final SpannableString span = new SpannableString(TextUtilsKt.toHtmlSpanned(htmlText));
            if (subtitlesBackgroundEnabled) {
                // Disable the text outlining when the background is enabled
                binding.subtitlesText.setStrokeWidth(0.0f);

                // get the alignment gravity of the TextView
                // extract the absolute horizontal gravity so the span can draw its background aligned
                int gravity = binding.subtitlesText.getGravity();
                int horizontalGravity = Gravity.getAbsoluteGravity(gravity, binding.subtitlesText.getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
                span.setSpan(new PaddedLineBackgroundSpan(ContextCompat.getColor(requireContext(), R.color.black_opaque), SUBTITLE_PADDING, horizontalGravity), 0, span.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            binding.subtitlesText.setText(span);
            binding.subtitlesText.setVisibility(View.VISIBLE);
        });
    }
}
