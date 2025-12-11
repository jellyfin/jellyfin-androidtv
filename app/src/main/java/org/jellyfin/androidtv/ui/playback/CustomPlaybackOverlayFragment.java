package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.OverlayTvGuideBinding;
import org.jellyfin.androidtv.databinding.VlcPlayerInterfaceBinding;
import org.jellyfin.androidtv.ui.GuideChannelHeader;
import org.jellyfin.androidtv.ui.GuidePagingButton;
import org.jellyfin.androidtv.ui.HorizontalScrollViewListener;
import org.jellyfin.androidtv.ui.LiveProgramDetailPopup;
import org.jellyfin.androidtv.ui.ObservableHorizontalScrollView;
import org.jellyfin.androidtv.ui.ObservableScrollView;
import org.jellyfin.androidtv.ui.ProgramGridCell;
import org.jellyfin.androidtv.ui.ScrollViewListener;
import org.jellyfin.androidtv.ui.itemhandling.ChapterItemInfoBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragment;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragmentHelperKt;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.ChannelCardPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.ChapterInfo;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import timber.log.Timber;

public class CustomPlaybackOverlayFragment extends Fragment implements LiveTvGuide, View.OnKeyListener {
    protected VlcPlayerInterfaceBinding binding;
    private OverlayTvGuideBinding tvGuideBinding;

    private RowsSupportFragment mPopupRowsFragment;
    private ListRow mChapterRow;
    private ArrayObjectAdapter mPopupRowAdapter;
    private PositionableListRowPresenter mPopupRowPresenter;

    //Live guide items
    private static final int PAGE_SIZE = 75;
    private static final int GUIDE_HOURS = 9;

    BaseItemDto mSelectedProgram;
    RelativeLayout mSelectedProgramView;
    private boolean mGuideVisible = false;
    private LocalDateTime mCurrentGuideStart;
    private LocalDateTime mCurrentGuideEnd;
    private int mCurrentDisplayChannelStartNdx = 0;
    private int mCurrentDisplayChannelEndNdx = 0;
    private List<BaseItemDto> mAllChannels;
    private UUID mFirstFocusChannelId;

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
    private boolean navigating = false;

    protected LeanbackOverlayFragment leanbackOverlayFragment;

    private final Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<VideoQueueManager> videoQueueManager = inject(VideoQueueManager.class);
    private final Lazy<PlaybackControllerContainer> playbackControllerContainer = inject(PlaybackControllerContainer.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);
    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<ImageHelper> imageHelper = inject(ImageHelper.class);

    private final PlaybackOverlayFragmentHelper helper = new PlaybackOverlayFragmentHelper(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // stop any audio that may be playing
        mediaManager.getValue().stopAudio(true);

        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Timber.e("Unable to get audio manager");
            Utils.showToast(requireContext(), R.string.msg_cannot_play_time);
            return;
        }

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mItemsToPlay = videoQueueManager.getValue().getCurrentVideoQueue();
        if (mItemsToPlay == null || mItemsToPlay.isEmpty()) return;

        int mediaPosition = videoQueueManager.getValue().getCurrentMediaPosition();

        playbackControllerContainer.getValue().setPlaybackController(new PlaybackController(mItemsToPlay, this, mediaPosition));

        // setup fade task
        mHideTask = () -> {
            if (mIsVisible) {
                leanbackOverlayFragment.hideOverlay();
            }
        };

        backgroundService.getValue().disable();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = VlcPlayerInterfaceBinding.inflate(inflater, container, false);
        binding.textClock.setVideoPlayer(true);

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

        if (mItemsToPlay == null || mItemsToPlay.isEmpty()) {
            Utils.showToast(requireContext(), getString(R.string.msg_no_playable_items));
            closePlayer();
            return;
        }

        PlaybackController playbackController = playbackControllerContainer.getValue().getPlaybackController();

        if (playbackController != null) {
            playbackController.init(new VideoManager(requireActivity(), view, helper), this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        // To fix race condition in hide timer
        mIsVisible = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mItemsToPlay == null || mItemsToPlay.isEmpty()) return;

        prepareOverlayFragment();

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_top_in);
        slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_bottom_in);
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
        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.ActionComplete.INSTANCE)) dismissProgramOptions();
            return null;
        });

        int startPos = getArguments().getInt("Position", 0);

        // start playing
        playbackControllerContainer.getValue().getPlaybackController().play(startPos);
        leanbackOverlayFragment.updatePlayState();

        // Set initial skip overlay state
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void prepareOverlayFragment() {
        leanbackOverlayFragment = (LeanbackOverlayFragment) getChildFragmentManager().findFragmentById(R.id.leanback_fragment);
        if (leanbackOverlayFragment != null) {
            leanbackOverlayFragment.initFromView(this);
            leanbackOverlayFragment.mediaInfoChanged();
            leanbackOverlayFragment.setOnKeyInterceptListener(keyListener);
        }
    }

    private void setupPopupAnimations() {
        showPopup = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_bottom_in);
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
        hidePopup = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out);
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
                    playbackControllerContainer.getValue().getPlaybackController().pause();
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
            if (item instanceof ChapterItemInfoBaseRowItem) {
                ChapterItemInfoBaseRowItem rowItem = (ChapterItemInfoBaseRowItem) item;
                Long start = rowItem.getChapterInfo().getStartPositionTicks() / 10000;
                playbackControllerContainer.getValue().getPlaybackController().seek(start);
                hidePopupPanel();
            } else if (item instanceof BaseItemDto) {
                hidePopupPanel();
                switchChannel(((BaseItemDto) item).getId());
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
                if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) hide();
            } else if (mGuideVisible) {
                hideGuide();
            } else {
                closePlayer();
            }
        }
    };

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.isLongPress()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (mSelectedProgramView instanceof ProgramGridCell)
                    showProgramOptions();
                else if (mSelectedProgramView instanceof GuideChannelHeader)
                    CustomPlaybackOverlayFragmentHelperKt.toggleFavorite(this);
                return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                event.startTracking();
                return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyListener.onKey(v, keyCode, event)) return true;

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
                    if (mGuideVisible && mSelectedProgramView instanceof ProgramGridCell && mSelectedProgram != null && mSelectedProgram.getChannelId() != null) {
                        if (mSelectedProgram.getStartDate().isBefore(LocalDateTime.now()))
                            switchChannel(mSelectedProgram.getChannelId());
                        else
                            showProgramOptions();
                        return true;
                    } else if (mSelectedProgramView instanceof GuideChannelHeader) {
                        switchChannel(((GuideChannelHeader) mSelectedProgramView).getChannel().getId(), false);
                        return true;
                    }
                }
                return false;
            }

            PlaybackController playbackController = playbackControllerContainer.getValue().getPlaybackController();

            if (playbackController != null) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    playbackController.play(0);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    playbackController.pause();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    playbackController.playPause();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
                    playbackController.fastForward();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                    playbackController.rewind();
                    return true;
                }
            }
        }

        return false;
    }

    public void refreshFavorite(UUID channelId) {
        for (int i = 0; i < tvGuideBinding.channels.getChildCount(); i++) {
            GuideChannelHeader gch = (GuideChannelHeader) tvGuideBinding.channels.getChildAt(i);
            if (gch.getChannel().getId().equals(channelId.toString()))
                gch.refreshFavorite();
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

                if (binding.skipOverlay.getVisible()) {
                    // Hide without doing anything
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        clearSkipOverlay();
                        return true;
                    }

                    // Hide with seek
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        playbackControllerContainer.getValue().getPlaybackController().seek(binding.skipOverlay.getTargetPositionMs(), true);
                        leanbackOverlayFragment.setShouldShowOverlay(false);
                        if (binding != null) clearSkipOverlay();
                        return true;
                    }
                }

                if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                    closePlayer();
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    if (mPopupPanelVisible) {
                        // back should just hide the popup panel
                        hidePopupPanel();
                        leanbackOverlayFragment.hideOverlay();

                        // also close this if live tv
                        if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) hide();
                        return true;
                    } else if (mGuideVisible) {
                        hideGuide();
                        return true;
                    }
                }

                if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv() && !mPopupPanelVisible && !mGuideVisible && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
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

                if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv() && keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
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
                            if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv())
                                hide(); //also close this if live tv
                            return true;
                        } else {
                            return false;
                        }
                    }

                    // Control fast forward and rewind if overlay hidden and not showing live TV
                    if (!playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) {
                        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
                            playbackControllerContainer.getValue().getPlaybackController().fastForward();
                            setFadingEnabled(true);
                            return true;
                        }

                        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                            playbackControllerContainer.getValue().getPlaybackController().rewind();
                            setFadingEnabled(true);
                            return true;
                        }
                    }

                    if (!mIsVisible) {
                        if (!playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                setFadingEnabled(true);
                                return true;
                            }

                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                setFadingEnabled(true);
                                return true;
                            }
                        }

                        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                                && playbackControllerContainer.getValue().getPlaybackController().canSeek()) {
                            // if the player is playing and the overlay is hidden, this will pause
                            // if the player is paused and then 'back' is pressed to hide the overlay, this will play
                            playbackControllerContainer.getValue().getPlaybackController().playPause();
                            return true;
                        }
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

    public LocalDateTime getCurrentLocalStartDate() {
        return mCurrentGuideStart;
    }

    public LocalDateTime getCurrentLocalEndDate() {
        return mCurrentGuideEnd;
    }

    public void switchChannel(UUID id) {
        switchChannel(id, true);
    }

    public void switchChannel(UUID id, boolean hideGuide) {
        if (playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem().getId().equals(id)) {
            // same channel, just dismiss overlay
            if (hideGuide)
                hideGuide();
        } else {
            playbackControllerContainer.getValue().getPlaybackController().stop();
            if (hideGuide)
                hideGuide();

            CustomPlaybackOverlayFragmentHelperKt.playChannel(this, id);
        }
    }

    private void startFadeTimer() {
        mFadeEnabled = true;
        mHandler.removeCallbacks(mHideTask);
        mHandler.postDelayed(mHideTask, 6000);
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView()).hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Close player when resuming without a valid playback controller
        if (playbackControllerContainer.getValue().getPlaybackController() == null || !playbackControllerContainer.getValue().getPlaybackController().hasFragment()) {
            closePlayer();

            return;
        }

        // Hide system bars
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.e("Unable to get audio focus");
            Utils.showToast(requireContext(), R.string.msg_cannot_play_time);
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mItemsToPlay == null || mItemsToPlay.isEmpty()) return;

        setPlayPauseActionState(0);

        // give back audio focus
        mAudioManager.abandonAudioFocus(mAudioFocusChanged);
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.i("Stopping!");

        if (leanbackOverlayFragment != null)
            leanbackOverlayFragment.setOnKeyInterceptListener(null);

        // end playback from here if this fragment belongs to the current session.
        // if it doesn't, playback has already been stopped elsewhere, and the references to this have been replaced
        if (playbackControllerContainer.getValue().getPlaybackController() != null && playbackControllerContainer.getValue().getPlaybackController().getFragment() == this) {
            Timber.i("this fragment belongs to the current session, ending it");
            playbackControllerContainer.getValue().getPlaybackController().endPlayback();
        }

        closePlayer();
    }

    public void show() {
        // Already showing!
        if (mIsVisible) return;

        binding.topPanel.startAnimation(slideDown);
        mIsVisible = true;
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    public void hide() {
        // Can't hide what's already hidden
        if (!mIsVisible) return;

        mIsVisible = false;
        binding.topPanel.startAnimation(fadeOut);
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    private void showChapterPanel() {
        setFadingEnabled(false);
        binding.popupArea.startAnimation(showPopup);
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    private void hidePopupPanel() {
        startFadeTimer();
        binding.popupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    public void showGuide() {
        hide();
        leanbackOverlayFragment.setShouldShowOverlay(false);
        leanbackOverlayFragment.hideOverlay();
        playbackControllerContainer.getValue().getPlaybackController().mVideoManager.contractVideo(Utils.convertDpToPixel(requireContext(), 300));
        tvGuideBinding.getRoot().setVisibility(View.VISIBLE);
        mGuideVisible = true;
        LocalDateTime now = LocalDateTime.now();
        boolean needLoad = mCurrentGuideStart == null;
        if (!needLoad) {
            LocalDateTime needLoadTime = mCurrentGuideStart.plusMinutes(30);
            needLoad = now.isAfter(needLoadTime);
            if (mSelectedProgramView != null)
                mSelectedProgramView.requestFocus();
        }
        if (needLoad) {
            loadGuide();
        }
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    private void hideGuide() {
        tvGuideBinding.getRoot().setVisibility(View.GONE);
        playbackControllerContainer.getValue().getPlaybackController().mVideoManager.setVideoFullSize(true);
        mGuideVisible = false;
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
    }

    private void loadGuide() {
        tvGuideBinding.spinner.setVisibility(View.VISIBLE);
        fillTimeLine(GUIDE_HOURS);
        TvManager.loadAllChannels(this, ndx -> {
            if (ndx >= PAGE_SIZE) {
                // last channel is not in first page so grab a set where it will be in the middle
                ndx = ndx - (PAGE_SIZE / 2);
            } else {
                ndx = 0; // just start at beginning
            }

            mAllChannels = TvManager.getAllChannels();
            if (!mAllChannels.isEmpty()) {
                displayChannels(ndx, PAGE_SIZE);
            } else {
                tvGuideBinding.spinner.setVisibility(View.GONE);
            }

            return null;
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
        TvManager.getProgramsAsync(this, mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyResponse(getLifecycle()) {
            @Override
            public void onResponse() {
                if (!isActive()) return;
                Timber.d("*** Programs response");
                if (mDisplayProgramsTask != null) mDisplayProgramsTask.cancel(true);
                mDisplayProgramsTask = new DisplayProgramsTask(self);
                mDisplayProgramsTask.execute(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx);
            }
        });
        binding.skipOverlay.setSkipUiEnabled(!mIsVisible && !mGuideVisible && !mPopupPanelVisible);
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
            mFirstFocusChannelId = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem().getId();

            if (mCurrentDisplayChannelStartNdx > 0) {
                // Show a paging row for channels above
                int pageUpStart = mCurrentDisplayChannelStartNdx - PAGE_SIZE;
                if (pageUpStart < 0) pageUpStart = 0;

                TextView placeHolder = new TextView(requireContext());
                placeHolder.setHeight(Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_HEIGHT_DP));
                tvGuideBinding.channels.addView(placeHolder);
                displayedChannels = 0;

                String label = TextUtilsKt.getLoadChannelsLabel(requireContext(), mAllChannels.get(pageUpStart).getNumber(), mAllChannels.get(mCurrentDisplayChannelStartNdx - 1).getNumber());
                tvGuideBinding.programRows.addView(new GuidePagingButton(requireContext(), guide, pageUpStart, label));
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
                final BaseItemDto channel = TvManager.getChannel(i);
                List<BaseItemDto> programs = TvManager.getProgramsForChannel(channel.getId());
                final LinearLayout row = getProgramRow(programs, channel.getId());
                if (first) {
                    first = false;
                    firstRow = row;
                }

                // put focus on the last tuned channel
                if (channel.getId().equals(mFirstFocusChannelId.toString())) {
                    firstRow = row;
                    mFirstFocusChannelId = null; // only do this first time in not while paging around
                }

                // set focus parameters if we are not on first row
                // this makes focus movements more predictable for the grid view
                if (prevRow != null) {
                    TvManager.setFocusParams(row, prevRow, true);
                    TvManager.setFocusParams(prevRow, row, false);
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
                placeHolder.setHeight(Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_HEIGHT_DP));
                tvGuideBinding.channels.addView(placeHolder);

                String label = TextUtilsKt.getLoadChannelsLabel(requireContext(), mAllChannels.get(mCurrentDisplayChannelEndNdx + 1).getNumber(), mAllChannels.get(pageDnEnd).getNumber());
                tvGuideBinding.programRows.addView(new GuidePagingButton(requireContext(), guide, mCurrentDisplayChannelEndNdx + 1, label));
            }

            tvGuideBinding.channelsStatus.setText(getResources().getString(R.string.lbl_tv_channel_status, displayedChannels, mAllChannels.size()));
            tvGuideBinding.filterStatus.setText(getResources().getString(R.string.lbl_tv_filter_status, GUIDE_HOURS));

            tvGuideBinding.spinner.setVisibility(View.GONE);

            if (firstRow != null) firstRow.requestFocus();
        }
    }

    private int currentCellId = 0;

    private GuideChannelHeader getChannelHeader(Context context, BaseItemDto channel) {
        return new GuideChannelHeader(context, this, channel);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, UUID channelId) {
        int guideRowHeightPx = Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_HEIGHT_DP);
        int guideRowWidthPerMinPx = Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_WIDTH_PER_MIN_DP);

        LinearLayout programRow = new LinearLayout(requireContext());
        if (programs.size() == 0) {

            int minutes = ((Long) ((mCurrentGuideEnd.toInstant(ZoneOffset.UTC).toEpochMilli() - mCurrentGuideStart.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000)).intValue();
            int slot = 0;

            do {
                BaseItemDto empty = LiveTvGuideFragmentHelperKt.createNoProgramDataBaseItem(
                        getContext(),
                        channelId,
                        mCurrentGuideStart.plusMinutes(30l * slot),
                        mCurrentGuideEnd.plusMinutes(30l * (slot + 1))
                );
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(30 * guideRowWidthPerMinPx, guideRowHeightPx));
                programRow.addView(cell);
                if (slot == 0)
                    cell.setFirst();
                if (slot == (minutes / 30) - 1)
                    cell.setLast();
                slot++;
            } while ((30 * slot) < minutes);

            return programRow;
        }

        LocalDateTime prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            LocalDateTime start = item.getStartDate() != null ? item.getStartDate() : getCurrentLocalStartDate();
            if (start.isBefore(getCurrentLocalStartDate())) start = getCurrentLocalStartDate();
            if (start.isAfter(getCurrentLocalEndDate())) continue;
            if (start.isBefore(prevEnd)) continue;

            if (start.isAfter(prevEnd)) {
                BaseItemDto empty = LiveTvGuideFragmentHelperKt.createNoProgramDataBaseItem(
                        getContext(),
                        channelId,
                        prevEnd,
                        start
                );
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long) ((start.toInstant(ZoneOffset.UTC).toEpochMilli() - prevEnd.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                programRow.addView(cell);
            }
            LocalDateTime end = item.getEndDate() != null ? item.getEndDate() : getCurrentLocalEndDate();
            if (end.isAfter(getCurrentLocalEndDate())) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end.toInstant(ZoneOffset.UTC).toEpochMilli() - start.toInstant(ZoneOffset.UTC).toEpochMilli()) / 60000;
            if (duration > 0) {
                ProgramGridCell program = new ProgramGridCell(requireContext(), this, item, false);
                program.setId(currentCellId++);
                program.setLayoutParams(new ViewGroup.LayoutParams(duration.intValue() * guideRowWidthPerMinPx, guideRowHeightPx));

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
        mCurrentGuideStart = LocalDateTime.now();
        mCurrentGuideStart = mCurrentGuideStart
                .withMinute(mCurrentGuideStart.getMinute() >= 30 ? 30 : 0)
                .withSecond(0)
                .withNano(0);

        tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mCurrentGuideStart));
        mCurrentGuideEnd = mCurrentGuideStart
                .plusHours(hours);
        int oneHour = 60 * Utils.convertDpToPixel(requireContext(), 7);
        int halfHour = 30 * Utils.convertDpToPixel(requireContext(), 7);
        int interval = mCurrentGuideStart.getMinute() >= 30 ? 30 : 60;
        tvGuideBinding.timeline.removeAllViews();

        LocalDateTime current = mCurrentGuideStart;
        while (current.isBefore(mCurrentGuideEnd)) {
            TextView time = new TextView(requireContext());
            time.setText(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(current));
            time.setWidth(interval == 30 ? halfHour : oneHour);
            tvGuideBinding.timeline.addView(time);
            current = current.plusMinutes(interval);
            //after first one, we always go on hours
            interval = 60;
        }
    }

    private Runnable detailUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;
            CustomPlaybackOverlayFragmentHelperKt.refreshSelectedProgram(CustomPlaybackOverlayFragment.this);
        }
    };

    void detailUpdateInternal() {
        tvGuideBinding.guideTitle.setText(mSelectedProgram.getName());
        tvGuideBinding.summary.setText(mSelectedProgram.getOverview());
        //info row
        InfoLayoutHelper.addInfoRow(requireContext(), mSelectedProgram, tvGuideBinding.guideInfoRow, false);
        if (mSelectedProgram.getId() != null) {
            tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mSelectedProgram.getStartDate()));
        }

        if (mDetailPopup != null && mDetailPopup.isShowing() && mSelectedProgramView != null) {
            mDetailPopup.setContent(mSelectedProgram, ((ProgramGridCell) mSelectedProgramView));
        }
    }

    public void setSelectedProgram(RelativeLayout programView) {
        mSelectedProgramView = programView;
        if (mSelectedProgramView instanceof ProgramGridCell) {
            mSelectedProgram = ((ProgramGridCell) mSelectedProgramView).getProgram();
            mHandler.removeCallbacks(detailUpdateTask);
            mHandler.postDelayed(detailUpdateTask, 500);
        } else if (mSelectedProgramView instanceof GuideChannelHeader) {
            for (int i = 0; i < tvGuideBinding.channels.getChildCount(); i++) {
                if (mSelectedProgramView == tvGuideBinding.channels.getChildAt(i)) {
                    LinearLayout programRow = (LinearLayout) tvGuideBinding.programRows.getChildAt(i);
                    if (programRow == null)
                        return;
                    for (int ii = 0; ii < programRow.getChildCount(); ii++) {
                        ProgramGridCell prog = (ProgramGridCell) programRow.getChildAt(ii);
                        if (prog.getProgram() != null && prog.getProgram().getStartDate().isBefore(LocalDateTime.now()) && prog.getProgram().getEndDate().isAfter(LocalDateTime.now())) {
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
            mDetailPopup = new LiveProgramDetailPopup(requireActivity(), this, this, Utils.convertDpToPixel(requireContext(), 600), new EmptyResponse(getLifecycle()) {
                @Override
                public void onResponse() {
                    if (!isActive()) return;
                    switchChannel(mSelectedProgram.getChannelId());
                }
            });
        mDetailPopup.setContent(mSelectedProgram, (ProgramGridCell) mSelectedProgramView);
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
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

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
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            int ndx = getCurrentChapterIndex(playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem(), playbackControllerContainer.getValue().getPlaybackController().getCurrentPosition() * 10000);
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
            for (ChapterInfo chapter : item.getChapters()) {
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
                if (Utils.isTrue(program.isSeries())) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lbl_record_series)
                            .setMessage(R.string.msg_record_entire_series)
                            .setPositiveButton(R.string.lbl_record_series, (dialog, which) -> CustomPlaybackOverlayFragmentHelperKt.recordProgram(this, program, true))
                            .setNegativeButton(R.string.lbl_just_this_once, (dialog, which) -> CustomPlaybackOverlayFragmentHelperKt.recordProgram(this, program, false))
                            .show();
                } else {
                    CustomPlaybackOverlayFragmentHelperKt.recordProgram(this, program, false);
                }
            }
        }
    }

    private void cancelRecording(BaseItemDto program, boolean series) {
        if (program != null) {
            if (series) {
                CustomPlaybackOverlayFragmentHelperKt.cancelSeriesTimer(this, program.getSeriesTimerId());
            } else {
                CustomPlaybackOverlayFragmentHelperKt.cancelTimer(this, program.getTimerId());
            }
        }
    }

    public void setCurrentTime(long time) {
        binding.skipOverlay.setCurrentPositionMs(time);
        if (leanbackOverlayFragment != null)
            leanbackOverlayFragment.updateCurrentPosition();
    }

    public void setSecondaryTime(long time) {
    }

    public void setFadingEnabled(boolean value) {
        mFadeEnabled = value;
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
        BaseItemDto current = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem();
        if (current != null && getContext() != null) {
            leanbackOverlayFragment.mediaInfoChanged();
            leanbackOverlayFragment.onFullyInitialized();
            leanbackOverlayFragment.recordingStateChanged();
            // set progress to match duration
            // set other information
            tvGuideBinding.guideCurrentTitle.setText(current.getName());

            // Update the title and subtitle
            if (current.getType() == BaseItemKind.EPISODE) {
                binding.itemTitle.setText(current.getSeriesName());
                binding.itemSubtitle.setText(BaseItemExtensionsKt.getDisplayName(current, requireContext()));
            } else {
                binding.itemTitle.setText(current.getName());
            }
            // Update the logo
            String imageUrl = imageHelper.getValue().getLogoImageUrl(current, 440);
            if (imageUrl != null) {
                binding.itemLogo.setVisibility(View.VISIBLE);
                binding.itemTitle.setVisibility(View.GONE);
                binding.itemLogo.setContentDescription(current.getName());
                binding.itemLogo.load(imageUrl, null, null, 1.0, 0);
            } else {
                binding.itemLogo.setVisibility(View.GONE);
                binding.itemTitle.setVisibility(View.VISIBLE);
            }

            if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) {
                prepareChannelAdapter();
            } else {
                prepareChapterAdapter();
            }
        }
    }

    public void clearSkipOverlay() {
        binding.skipOverlay.setTargetPositionMs(null);
    }

    private void prepareChapterAdapter() {
        BaseItemDto item = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem();
        List<ChapterInfo> chapters = item.getChapters();

        if (chapters != null && !chapters.isEmpty()) {
            // create chapter row for later use
            ItemRowAdapter chapterAdapter = new ItemRowAdapter(requireContext(), BaseItemExtensionsKt.buildChapterItems(item, api.getValue()), new CardPresenter(true, 110), new MutableObjectAdapter<Row>());
            chapterAdapter.Retrieve();
            if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
            mChapterRow = new ListRow(new HeaderItem(requireContext().getString(R.string.chapters)), chapterAdapter);
            mPopupRowAdapter.add(mChapterRow);
        }

    }

    private void prepareChannelAdapter() {
        // create quick channel change row
        TvManager.loadAllChannels(this, response -> {
            List<BaseItemDto> channels = TvManager.getAllChannels();
            if (channels == null) return null;
            ArrayObjectAdapter channelAdapter = new ArrayObjectAdapter(new ChannelCardPresenter());
            channelAdapter.addAll(0, channels);
            if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
            mChapterRow = new ListRow(new HeaderItem(requireContext().getString(R.string.channels)), channelAdapter);
            mPopupRowAdapter.add(mChapterRow);
            return null;
        });
    }

    public void closePlayer() {
        if (navigating) return;
        navigating = true;

        if (navigationRepository.getValue().getCanGoBack()) {
            navigationRepository.getValue().goBack();
        } else {
            navigationRepository.getValue().reset(Destinations.INSTANCE.getHome());
        }
    }

    public void showNextUp(@NonNull UUID id) {
        if (navigating) return;
        navigating = true;

        navigationRepository.getValue().navigate(Destinations.INSTANCE.nextUp(id), true);
    }

    public void showStillWatching(@NonNull UUID id) {
        if (navigating) return;
        navigating = true;

        navigationRepository.getValue().navigate(Destinations.INSTANCE.stillWatching(id), true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Show system bars
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), true);
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView()).show(WindowInsetsCompat.Type.systemBars());

        // Reset display mode
        WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
        params.preferredDisplayModeId = 0;
        getActivity().getWindow().setAttributes(params);
    }
}
