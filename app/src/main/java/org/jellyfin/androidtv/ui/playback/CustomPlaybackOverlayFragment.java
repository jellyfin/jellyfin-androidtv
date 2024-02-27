package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.app.AlertDialog;
import android.content.Context;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import org.jellyfin.androidtv.auth.repository.UserRepository;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.data.model.DataRefreshService;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
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
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragment;
import org.jellyfin.androidtv.ui.livetv.TvManager;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.ChannelCardPresenter;
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.PaddedLineBackgroundSpan;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.TextUtilsKt;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyLifecycleAwareResponse;
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackEvent;
import org.jellyfin.apiclient.model.mediainfo.SubtitleTrackInfo;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.ChapterInfo;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import kotlin.Lazy;
import timber.log.Timber;

public class CustomPlaybackOverlayFragment extends Fragment implements LiveTvGuide, View.OnKeyListener {
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

    private List<org.jellyfin.sdk.model.api.BaseItemDto> mItemsToPlay;

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
    private static final int SUBTITLE_PADDING = 20;
    private static final long SUBTITLE_RENDER_INTERVAL_MS = 50;
    private SubtitleTrackInfo subtitleTrackInfo;
    private int currentSubtitleIndex = 0;
    private long lastSubtitlePositionMs = 0;
    private final UserPreferences userPreferences = KoinJavaComponent.<UserPreferences>get(UserPreferences.class);
    private final int subtitlesSize = userPreferences.get(UserPreferences.Companion.getSubtitlesSize());
    private final boolean subtitlesBackgroundEnabled = userPreferences.get(UserPreferences.Companion.getSubtitlesBackgroundEnabled());
    private final int subtitlesPosition = userPreferences.get(UserPreferences.Companion.getSubtitlePosition());
    private final int subtitlesStrokeWidth = userPreferences.get(UserPreferences.Companion.getSubtitleStrokeSize());

    private final Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private final Lazy<org.jellyfin.sdk.api.client.ApiClient> api = inject(org.jellyfin.sdk.api.client.ApiClient.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<VideoQueueManager> videoQueueManager = inject(VideoQueueManager.class);
    private final Lazy<PlaybackControllerContainer> playbackControllerContainer = inject(PlaybackControllerContainer.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);
    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);

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
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) {
            Utils.showToast(requireContext(), getString(R.string.msg_no_playable_items));
            closePlayer();
            return;
        }

        int mediaPosition = videoQueueManager.getValue().getCurrentMediaPosition();

        playbackControllerContainer.getValue().setPlaybackController(new PlaybackController(mItemsToPlay, this, mediaPosition));

        // setup fade task
        mHideTask = () -> {
            if (mIsVisible) {
                hide();
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
            playbackControllerContainer.getValue().getPlaybackController().init(new VideoManager((requireActivity()), view, helper), this);
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
        if (mItemsToPlay == null || mItemsToPlay.size() == 0) return;

        prepareOverlayFragment();

        //manual subtitles
        // This configuration is required for the PaddedLineBackgroundSpan to work
        binding.subtitlesText.setShadowLayer(SUBTITLE_PADDING, 0, 0, Color.TRANSPARENT);
        binding.subtitlesText.setPadding(SUBTITLE_PADDING, 0, SUBTITLE_PADDING, 0);

        // Subtitles font size configuration
        binding.subtitlesText.setTextSize(subtitlesSize);

        // Subtitles font position (margin bottom)
        if (subtitlesPosition > 0) {
            ViewGroup.MarginLayoutParams currentLayoutParams = (ViewGroup.MarginLayoutParams) binding.subtitlesText.getLayoutParams();
            currentLayoutParams.bottomMargin = (8 + Utils.convertDpToPixel(requireContext(), subtitlesPosition));
            binding.subtitlesText.setLayoutParams(currentLayoutParams);
        }

        // Subtitles stroke width
        if (subtitlesStrokeWidth > 0 && !subtitlesBackgroundEnabled) {
            binding.subtitlesText.setStrokeWidth(subtitlesStrokeWidth);
        }

        //pre-load animations
        fadeOut = AnimationUtils.loadAnimation(requireContext(), androidx.leanback.R.anim.abc_fade_out);
        fadeOut.setAnimationListener(hideAnimationListener);
        slideDown = AnimationUtils.loadAnimation(requireContext(), androidx.leanback.R.anim.abc_slide_in_top);
        slideUp = AnimationUtils.loadAnimation(requireContext(), androidx.leanback.R.anim.abc_slide_in_bottom);
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

        requireActivity().getOnBackPressedDispatcher().addCallback(backPressedCallback);

        int startPos = getArguments().getInt("Position", 0);

        // start playing
        playbackControllerContainer.getValue().getPlaybackController().play(startPos);
        leanbackOverlayFragment.updatePlayState();

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
        showPopup = AnimationUtils.loadAnimation(requireContext(), androidx.leanback.R.anim.abc_slide_in_bottom);
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
        hidePopup = AnimationUtils.loadAnimation(requireContext(), androidx.leanback.R.anim.abc_fade_out);
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
            if (item instanceof BaseRowItem) {
                BaseRowItem rowItem = (BaseRowItem) item;
                switch (rowItem.getBaseRowType()) {
                    case Chapter:
                        Long start = rowItem.getChapterInfo().getStartPositionTicks() / 10000;
                        playbackControllerContainer.getValue().getPlaybackController().seek(start);
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
                    toggleFavorite();
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
                        Date curUTC = TimeUtils.convertToUtcDate(new Date());
                        if (mSelectedProgram.getStartDate().before(curUTC))
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

            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                playbackControllerContainer.getValue().getPlaybackController().play(0);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                playbackControllerContainer.getValue().getPlaybackController().pause();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                playbackControllerContainer.getValue().getPlaybackController().playPause();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
                playbackControllerContainer.getValue().getPlaybackController().fastForward();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                playbackControllerContainer.getValue().getPlaybackController().rewind();
                return true;
            }
        }

        return false;
    }

    public void refreshFavorite(String channelId) {
        for (int i = 0; i < tvGuideBinding.channels.getChildCount(); i++) {
            GuideChannelHeader gch = (GuideChannelHeader) tvGuideBinding.channels.getChildAt(i);
            if (gch.getChannel().getId().equals(channelId))
                gch.refreshFavorite();
        }
    }

    private void toggleFavorite() {
        GuideChannelHeader header = (GuideChannelHeader) mSelectedProgramView;
        UserItemDataDto data = header.getChannel().getUserData();
        if (data != null) {
            apiClient.getValue().UpdateFavoriteStatusAsync(header.getChannel().getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), !data.getIsFavorite(), new LifecycleAwareResponse<UserItemDataDto>(getLifecycle()) {
                @Override
                public void onResponse(UserItemDataDto response) {
                    if (!getActive()) return;

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
                            if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) hide(); //also close this if live tv
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
        if (playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem().getId().equals(id)) {
            // same channel, just dismiss overlay
            if (hideGuide)
                hideGuide();
        } else {
            playbackControllerContainer.getValue().getPlaybackController().stop();
            if (hideGuide)
                hideGuide();
            apiClient.getValue().GetItemAsync(id, KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                @Override
                public void onResponse(BaseItemDto response) {
                    if (!getActive()) return;

                    List<org.jellyfin.sdk.model.api.BaseItemDto> items = new ArrayList<org.jellyfin.sdk.model.api.BaseItemDto>();
                    items.add(ModelCompat.asSdk(response));
                    playbackControllerContainer.getValue().getPlaybackController().setItems(items);
                    playbackControllerContainer.getValue().getPlaybackController().play(0);
                }

                @Override
                public void onError(Exception exception) {
                    if (!getActive()) return;

                    Utils.showToast(requireContext(), R.string.msg_video_playback_error);
                    closePlayer();
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

        // Close player when resuming without a valid playback contoller
        if (!playbackControllerContainer.getValue().getPlaybackController().hasFragment()) {
            if (navigationRepository.getValue().getCanGoBack()) {
                navigationRepository.getValue().goBack();
            } else {
                navigationRepository.getValue().reset(Destinations.INSTANCE.getHome());
            }

            return;
        }

        // Hide system bars
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView()).hide(WindowInsetsCompat.Type.systemBars());

        if (mAudioManager.requestAudioFocus(mAudioFocusChanged, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.e("Unable to get audio focus");
            Utils.showToast(requireContext(), R.string.msg_cannot_play_time);
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

        // end playback from here if this fragment belongs to the current session.
        // if it doesn't, playback has already been stopped elsewhere, and the references to this have been replaced
        if (playbackControllerContainer.getValue().getPlaybackController() != null && playbackControllerContainer.getValue().getPlaybackController().getFragment() == this) {
            Timber.d("this fragment belongs to the current session, ending it");
            playbackControllerContainer.getValue().getPlaybackController().endPlayback();
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
        playbackControllerContainer.getValue().getPlaybackController().mVideoManager.contractVideo(Utils.convertDpToPixel(requireContext(), 300));
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
        playbackControllerContainer.getValue().getPlaybackController().mVideoManager.setVideoFullSize(true);
        mGuideVisible = false;
    }

    private void loadGuide() {
        tvGuideBinding.spinner.setVisibility(View.VISIBLE);
        fillTimeLine(GUIDE_HOURS);
        TvManager.loadAllChannels(new LifecycleAwareResponse<Integer>(getLifecycle()) {
            @Override
            public void onResponse(Integer ndx) {
                if (!getActive()) return;

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
        TvManager.getProgramsAsync(mCurrentDisplayChannelStartNdx, mCurrentDisplayChannelEndNdx, mCurrentGuideStart, mCurrentGuideEnd, new EmptyLifecycleAwareResponse(getLifecycle()) {
            @Override
            public void onResponse() {
                if (!getActive()) return;

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
            mFirstFocusChannelId = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem().getId().toString();

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

    private GuideChannelHeader getChannelHeader(Context context, ChannelInfoDto channel) {
        return new GuideChannelHeader(context, this, channel);
    }

    private LinearLayout getProgramRow(List<BaseItemDto> programs, String channelId) {
        int guideRowHeightPx = Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_HEIGHT_DP);
        int guideRowWidthPerMinPx = Utils.convertDpToPixel(requireContext(), LiveTvGuideFragment.GUIDE_ROW_WIDTH_PER_MIN_DP);

        LinearLayout programRow = new LinearLayout(requireContext());
        if (programs.size() == 0) {

            int minutes = ((Long) ((mCurrentLocalGuideEnd - mCurrentLocalGuideStart) / 60000)).intValue();
            int slot = 0;

            do {
                BaseItemDto empty = new BaseItemDto();
                empty.setId(UUID.randomUUID().toString());
                empty.setBaseItemType(BaseItemType.Folder);
                empty.setName(getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30 * slot) * 60000))));
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(mCurrentLocalGuideStart + ((30 * (slot + 1)) * 60000))));
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

        long prevEnd = getCurrentLocalStartDate();
        for (BaseItemDto item : programs) {
            long start = item.getStartDate() != null ? TimeUtils.convertToLocalDate(item.getStartDate()).getTime() : getCurrentLocalStartDate();
            if (start < getCurrentLocalStartDate()) start = getCurrentLocalStartDate();
            if (start > getCurrentLocalEndDate()) continue;
            if (start < prevEnd) continue;

            if (start > prevEnd) {
                // fill empty time slot
                BaseItemDto empty = new BaseItemDto();
                empty.setId(UUID.randomUUID().toString());
                empty.setBaseItemType(BaseItemType.Folder);
                empty.setName(getString(R.string.no_program_data));
                empty.setChannelId(channelId);
                empty.setStartDate(TimeUtils.convertToUtcDate(new Date(prevEnd)));
                Long duration = (start - prevEnd);
                empty.setEndDate(TimeUtils.convertToUtcDate(new Date(prevEnd + duration)));
                ProgramGridCell cell = new ProgramGridCell(requireContext(), this, empty, false);
                cell.setId(currentCellId++);
                cell.setLayoutParams(new ViewGroup.LayoutParams(((Long) (duration / 60000)).intValue() * guideRowWidthPerMinPx, guideRowHeightPx));
                programRow.addView(cell);
            }
            long end = item.getEndDate() != null ? TimeUtils.convertToLocalDate(item.getEndDate()).getTime() : getCurrentLocalEndDate();
            if (end > getCurrentLocalEndDate()) end = getCurrentLocalEndDate();
            prevEnd = end;
            Long duration = (end - start) / 60000;
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
        mCurrentGuideStart = Calendar.getInstance();
        mCurrentGuideStart.set(Calendar.MINUTE, mCurrentGuideStart.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        mCurrentGuideStart.set(Calendar.SECOND, 0);
        mCurrentGuideStart.set(Calendar.MILLISECOND, 0);
        mCurrentLocalGuideStart = mCurrentGuideStart.getTimeInMillis();

        tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), mCurrentGuideStart.getTime()));
        Calendar current = (Calendar) mCurrentGuideStart.clone();
        mCurrentGuideEnd = (Calendar) mCurrentGuideStart.clone();
        int oneHour = 60 * Utils.convertDpToPixel(requireContext(), 7);
        int halfHour = 30 * Utils.convertDpToPixel(requireContext(), 7);
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
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            if (mSelectedProgram.getOverview() == null && mSelectedProgram.getId() != null) {
                apiClient.getValue().GetItemAsync(mSelectedProgram.getId(), KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue().getId().toString(), new LifecycleAwareResponse<BaseItemDto>(getLifecycle()) {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        if (!getActive()) return;

                        mSelectedProgram = response;
                        detailUpdateInternal();
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!getActive()) return;

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
        InfoLayoutHelper.addInfoRow(requireContext(), ModelCompat.asSdk(mSelectedProgram), tvGuideBinding.guideInfoRow, false);
        if (mSelectedProgram.getId() != null) {
            tvGuideBinding.displayDate.setText(TimeUtils.getFriendlyDate(requireContext(), TimeUtils.convertToLocalDate(mSelectedProgram.getStartDate())));
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
                    Date utcTime = TimeUtils.convertToUtcDate(new Date());
                    for (int ii = 0; ii < programRow.getChildCount(); ii++) {
                        ProgramGridCell prog = (ProgramGridCell) programRow.getChildAt(ii);
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
            mDetailPopup = new LiveProgramDetailPopup(requireActivity(), getLifecycle(), this, Utils.convertDpToPixel(requireContext(), 600), new EmptyLifecycleAwareResponse(getLifecycle()) {
                @Override
                public void onResponse() {
                    if (!getActive()) return;

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

    private int getCurrentChapterIndex(org.jellyfin.sdk.model.api.BaseItemDto item, long pos) {
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

    public void toggleRecording(org.jellyfin.sdk.model.api.BaseItemDto item) {
        final org.jellyfin.sdk.model.api.BaseItemDto program = item.getCurrentProgram();

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
                            .setPositiveButton(R.string.lbl_record_series, (dialog, which) -> recordProgram(program, true))
                            .setNegativeButton(R.string.lbl_just_this_once, (dialog, which) -> recordProgram(program, false))
                            .show();
                } else {
                    recordProgram(program, false);
                }
            }
        }
    }

    private void cancelRecording(org.jellyfin.sdk.model.api.BaseItemDto program, boolean series) {
        if (program != null) {
            if (series) {
                apiClient.getValue().CancelLiveTvSeriesTimerAsync(program.getSeriesTimerId(), new EmptyLifecycleAwareResponse(getLifecycle()) {
                    @Override
                    public void onResponse() {
                        if (!getActive()) return;

                        Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                        playbackControllerContainer.getValue().getPlaybackController().updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        if (!getActive()) return;

                        Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
                    }
                });
            } else {
                apiClient.getValue().CancelLiveTvTimerAsync(program.getTimerId(), new EmptyLifecycleAwareResponse(getLifecycle()) {
                    @Override
                    public void onResponse() {
                        if (!getActive()) return;

                        Utils.showToast(requireContext(), R.string.msg_recording_cancelled);
                        playbackControllerContainer.getValue().getPlaybackController().updateTvProgramInfo();
                        TvManager.forceReload();
                    }

                    @Override
                    public void onError(Exception ex) {
                        if (!getActive()) return;

                        Utils.showToast(requireContext(), R.string.msg_unable_to_cancel);
                    }
                });
            }
        }
    }

    private void recordProgram(final org.jellyfin.sdk.model.api.BaseItemDto program, final boolean series) {
        if (program != null) {
            apiClient.getValue().GetDefaultLiveTvTimerInfo(new LifecycleAwareResponse<SeriesTimerInfoDto>(getLifecycle()) {
                @Override
                public void onResponse(SeriesTimerInfoDto response) {
                    if (!getActive()) return;

                    response.setProgramId(program.getId().toString());
                    if (series) {
                        apiClient.getValue().CreateLiveTvSeriesTimerAsync(response, new EmptyLifecycleAwareResponse(getLifecycle()) {
                            @Override
                            public void onResponse() {
                                if (!getActive()) return;

                                Utils.showToast(requireContext(), R.string.msg_set_to_record);
                                playbackControllerContainer.getValue().getPlaybackController().updateTvProgramInfo();
                                TvManager.forceReload();
                            }

                            @Override
                            public void onError(Exception ex) {
                                if (!getActive()) return;

                                Utils.showToast(requireContext(), R.string.msg_unable_to_create_recording);
                            }
                        });
                    } else {
                        apiClient.getValue().CreateLiveTvTimerAsync(response, new EmptyLifecycleAwareResponse(getLifecycle()) {
                            @Override
                            public void onResponse() {
                                if (!getActive()) return;

                                Utils.showToast(requireContext(), R.string.msg_set_to_record);
                                playbackControllerContainer.getValue().getPlaybackController().updateTvProgramInfo();
                                TvManager.forceReload();
                            }

                            @Override
                            public void onError(Exception ex) {
                                if (!getActive()) return;

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
        org.jellyfin.sdk.model.api.BaseItemDto current = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem();
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

            if (playbackControllerContainer.getValue().getPlaybackController().isLiveTv()) {
                prepareChannelAdapter();
            } else {
                prepareChapterAdapter();
            }
        }
    }

    private void prepareChapterAdapter() {
        org.jellyfin.sdk.model.api.BaseItemDto item = playbackControllerContainer.getValue().getPlaybackController().getCurrentlyPlayingItem();
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
        TvManager.loadAllChannels(new LifecycleAwareResponse<Integer>(getLifecycle()) {
            @Override
            public void onResponse(Integer response) {
                if (!getActive()) return;

                ArrayObjectAdapter channelAdapter = new ArrayObjectAdapter(new ChannelCardPresenter());
                channelAdapter.addAll(0, TvManager.getAllChannels());
                if (mChapterRow != null) mPopupRowAdapter.remove(mChapterRow);
                mChapterRow = new ListRow(new HeaderItem("Channels"), channelAdapter);
                mPopupRowAdapter.add(mChapterRow);
            }
        });
    }

    public void closePlayer() {
        if (navigationRepository.getValue().getCanGoBack()) {
            navigationRepository.getValue().goBack();
        } else {
            navigationRepository.getValue().reset(Destinations.INSTANCE.getHome());
        }
    }

    public void showNextUp(@NonNull UUID id) {
        navigationRepository.getValue().navigate(Destinations.INSTANCE.nextUp(id), true);
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
            final String htmlText = text
                    // Encode whitespace as html entities
                    .replaceAll("\\r\\n", "<br>")
                    .replaceAll("\\n", "<br>")
                    .replaceAll("\\\\h", "&ensp;")
                    // Remove SSA tags
                    .replaceAll("\\{\\\\.*?\\}", "");

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Show system bars
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), true);
        WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView()).show(WindowInsetsCompat.Type.systemBars());
    }
}
