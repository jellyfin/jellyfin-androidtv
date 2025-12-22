package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwnerKt;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.FragmentAudioNowPlayingBinding;
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.navigation.Destinations;
import org.jellyfin.androidtv.ui.navigation.NavigationRepository;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.playback.core.PlaybackManager;

import java.util.List;

import kotlin.Lazy;
import timber.log.Timber;

public class AudioNowPlayingFragment extends Fragment {
    private TextView mGenreRow;
    private ImageButton mPlayPauseButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mAlbumButton;
    private ImageButton mArtistButton;
    private TextView mCounter;
    private NestedScrollView mScrollView;

    private DisplayMetrics mMetrics;

    private TextView mArtistName;
    private TextView mSongTitle;
    private TextView mAlbumTitle;
    private TextView mCurrentNdx;
    private TextView mCurrentPos;
    private TextView mRemainingTime;
    private RowsSupportFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private PositionableListRowPresenter mAudioQueuePresenter;

    private org.jellyfin.sdk.model.api.BaseItemDto mBaseItem;
    private ListRow mQueueRow;

    private boolean queueRowHasFocus = false;

    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<MediaManager> mediaManager = inject(MediaManager.class);
    private final Lazy<PlaybackManager> playbackManager = inject(PlaybackManager.class);
    private final Lazy<NavigationRepository> navigationRepository = inject(NavigationRepository.class);
    private final Lazy<KeyProcessor> keyProcessor = inject(KeyProcessor.class);

    private PopupMenu popupMenu;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentAudioNowPlayingBinding binding = FragmentAudioNowPlayingBinding.inflate(getLayoutInflater(), container, false);

        binding.clock.setVideoPlayer(true);

        mArtistName = binding.artistTitle;
        mGenreRow = binding.genreRow;
        mSongTitle = binding.song;
        mAlbumTitle = binding.album;
        mCurrentNdx = binding.track;
        mScrollView = binding.mainScroller;
        mCounter = binding.counter;

        AudioNowPlayingFragmentHelperKt.initializePreviewView(binding.preview, playbackManager.getValue());

        ImageButton rewindButton = binding.rewindBtn;
        rewindButton.setContentDescription(getString(R.string.rewind));
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().rewind();
                updateButtons();
            }
        });
        rewindButton.setOnFocusChangeListener(mainAreaFocusListener);

        mPlayPauseButton = binding.playPauseBtn;
        mPlayPauseButton.setContentDescription(getString(R.string.lbl_pause));
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().togglePlayPause();
            }
        });
        mPlayPauseButton.setOnFocusChangeListener(mainAreaFocusListener);

        ImageButton fastForwardButton = binding.fastForwardBtn;
        fastForwardButton.setContentDescription(getString(R.string.fast_forward));
        fastForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().fastForward();
                updateButtons();
            }
        });
        fastForwardButton.setOnFocusChangeListener(mainAreaFocusListener);

        mPrevButton = binding.prevBtn;
        mPrevButton.setContentDescription(getString(R.string.lbl_prev_item));
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().prevAudioItem();
            }
        });
        mPrevButton.setOnFocusChangeListener(mainAreaFocusListener);

        mNextButton = binding.nextBtn;
        mNextButton.setContentDescription(getString(R.string.lbl_next_item));
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().nextAudioItem();
            }
        });
        mNextButton.setOnFocusChangeListener(mainAreaFocusListener);

        mRepeatButton = binding.repeatBtn;
        mRepeatButton.setContentDescription(getString(R.string.lbl_repeat));
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().toggleRepeat();
                updateButtons();
            }
        });
        mRepeatButton.setOnFocusChangeListener(mainAreaFocusListener);

        mShuffleButton = binding.shuffleBtn;
        mShuffleButton.setContentDescription(getString(R.string.lbl_shuffle_queue));
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().shuffleAudioQueue();
                updateButtons();
            }
        });
        mShuffleButton.setOnFocusChangeListener(mainAreaFocusListener);

        mAlbumButton = binding.albumBtn;
        mAlbumButton.setContentDescription(getString(R.string.lbl_open_album));
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationRepository.getValue().navigate(Destinations.INSTANCE.itemList(mBaseItem.getAlbumId()));
            }
        });
        mAlbumButton.setOnFocusChangeListener(mainAreaFocusListener);

        mArtistButton = binding.artistBtn;
        mArtistButton.setContentDescription(getString(R.string.lbl_open_artist));
        mArtistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBaseItem.getAlbumArtists() != null && !mBaseItem.getAlbumArtists().isEmpty()) {
                    navigationRepository.getValue().navigate(Destinations.INSTANCE.itemDetails(mBaseItem.getAlbumArtists().get(0).getId()));
                }
            }
        });
        mArtistButton.setOnFocusChangeListener(mainAreaFocusListener);

        AudioNowPlayingFragmentHelperKt.initializePlayerProgress(binding.playerProgress, playbackManager.getValue());
        binding.playerProgress.setOnFocusChangeListener(mainAreaFocusListener);

        mCurrentPos = binding.currentPos;
        mRemainingTime = binding.remainingTime;

        mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsSupportFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mAudioQueuePresenter = new PositionableListRowPresenter(10);
        mRowsAdapter = new ArrayObjectAdapter(mAudioQueuePresenter);
        mRowsFragment.setAdapter(mRowsAdapter);
        addQueue();


        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPlayPauseButton.requestFocus();
    }

    protected void addQueue() {
        mQueueRow = new ListRow(new HeaderItem(getString(R.string.current_queue)), new AudioQueueBaseRowAdapter(playbackManager.getValue(), LifecycleOwnerKt.getLifecycleScope(this)));
        mRowsAdapter.add(mQueueRow);
    }

    @Override
    public void onResume() {
        super.onResume();
        //link events
        mediaManager.getValue().addAudioEventListener(audioEventListener);
        loadItem();
        updateButtons();

        // load the item duration and set the position to 0 since it won't be set elsewhere until playback is initialized
        if (!mediaManager.getValue().isAudioPlayerInitialized())
            setCurrentTime(0, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissPopup();
        mediaManager.getValue().removeAudioEventListener(audioEventListener);
    }

    private AudioEventListener audioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(@NonNull PlaybackController.PlaybackState newState, @Nullable org.jellyfin.sdk.model.api.BaseItemDto currentItem) {
            Timber.d("**** Got playstate change: %s", newState.toString());
            if (currentItem != mBaseItem) loadItem();
            updateButtons();
        }

        @Override
        public void onProgress(long pos, long duration) {
            setCurrentTime(pos, duration);
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            Timber.d("Queue status changed (hasQueue=%s)", hasQueue);
            loadItem();
            if (mediaManager.getValue().isAudioPlayerInitialized()) {
                updateButtons();
            }
        }

        @Override
        public void onQueueReplaced() {
            dismissPopup();
        }
    };

    private View.OnFocusChangeListener mainAreaFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            View rowsFragmentView = mRowsFragment.getView();
            if (rowsFragmentView == null) return;

            if (mRowsFragment.getView().hasFocus()) {
                if (!queueRowHasFocus) {
                    mScrollView.smoothScrollTo(0, mScrollView.getBottom());
                }

                queueRowHasFocus = true;
            } else {
                if (queueRowHasFocus) {
                    mScrollView.smoothScrollTo(0, 0);
                }

                queueRowHasFocus = false;
            }
        }
    };

    private void loadItem() {
        dismissPopup();
        mBaseItem = mediaManager.getValue().getCurrentAudioItem();
        if (mBaseItem != null) {
            updateInfo(mBaseItem);
        } else {
            if (navigationRepository.getValue().getCanGoBack()) navigationRepository.getValue().goBack();
            else navigationRepository.getValue().navigate(Destinations.INSTANCE.getHome());
        }
    }

    private void updateButtons() {
        if (getActivity() == null) return;
        Timber.d("Updating buttons");
        boolean playing = mediaManager.getValue().isPlayingAudio();
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

                if (!playing) {
                    mPlayPauseButton.setImageResource(R.drawable.ic_play);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_play));
                } else {
                    mPlayPauseButton.setImageResource(R.drawable.ic_pause);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_pause));
                }
                mRepeatButton.setActivated(mediaManager.getValue().isRepeatMode());
                mPrevButton.setEnabled(mediaManager.getValue().hasPrevAudioItem());
                mNextButton.setEnabled(mediaManager.getValue().hasNextAudioItem());
                mShuffleButton.setEnabled(mediaManager.getValue().getCurrentAudioQueueSize() > 1);
                mShuffleButton.setActivated(mediaManager.getValue().isShuffleMode());
                if (mBaseItem != null) {
                    mAlbumButton.setEnabled(mBaseItem.getAlbumId() != null);
                    mArtistButton.setEnabled(mBaseItem.getAlbumArtists() != null && !mBaseItem.getAlbumArtists().isEmpty());
                }
            }
        });
    }

    private String getArtistName(org.jellyfin.sdk.model.api.BaseItemDto item) {
        String artistName = item.getArtists() != null && !item.getArtists().isEmpty() ? item.getArtists().get(0) : item.getAlbumArtist();
        return artistName != null ? artistName : "";
    }

    private void updateInfo(org.jellyfin.sdk.model.api.BaseItemDto item) {
        if (item != null) {
            mArtistName.setText(getArtistName(item));
            mSongTitle.setText(item.getName());
            if (item.getAlbum() != null) {
                mAlbumTitle.setText(getString(R.string.lbl_now_playing_album, item.getAlbum()));
            } else {
                mAlbumTitle.setText(null);
            }
            mCurrentNdx.setText(getString(R.string.lbl_now_playing_track, mediaManager.getValue().getCurrentAudioQueueDisplayPosition(), mediaManager.getValue().getCurrentAudioQueueDisplaySize()));
            addGenres(mGenreRow);
            backgroundService.getValue().setBackground(item);
        }
    }

    public void setCurrentTime(long time, long duration) {
        // Round the time to seconds so both the position and remaining times are in sync
        time = Math.round(time / 1000L) * 1000L;

        mCurrentPos.setText(TimeUtils.formatMillis(time));

        if (duration == 0L) mRemainingTime.setText(null);
        else mRemainingTime.setText("-" + TimeUtils.formatMillis(duration - time));
    }

    private void addGenres(TextView textView) {
        List<String> genres = mBaseItem.getGenres();
        textView.setText(genres == null ? "" : TextUtils.join(" / ", genres));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            popupMenu = keyProcessor.getValue().createItemMenu((BaseRowItem) item, ((BaseRowItem) item).getBaseItem().getUserData(), requireActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof AudioQueueBaseRowItem) {
                // Keep counter
                AudioQueueBaseRowAdapter adapter = (AudioQueueBaseRowAdapter) mQueueRow.getAdapter();
                mCounter.setText((adapter.indexOf((AudioQueueBaseRowItem) item) + 1) + " | " + adapter.size());
            }
        }
    }

    private void dismissPopup() {
        if (popupMenu != null) {
            popupMenu.dismiss();
            popupMenu = null;
        }
    }
}
