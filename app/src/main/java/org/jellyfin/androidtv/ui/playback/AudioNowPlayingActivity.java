package org.jellyfin.androidtv.ui.playback;

import static org.koin.java.KoinJavaComponent.inject;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.ui.ClockUserView;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import kotlin.Lazy;
import timber.log.Timber;

public class AudioNowPlayingActivity extends BaseActivity {
    private TextView mGenreRow;
    private ImageButton mPlayPauseButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mAlbumButton;
    private ImageButton mArtistButton;
    private ImageButton mSaveButton;
    private ClockUserView mClock;
    private TextView mCounter;
    private ScrollView mScrollView;
    private ImageView mLogoImage;

    private RelativeLayout mSSArea;
    private TextView mSSTime;
    private TextView mSSAlbumSong;
    private TextView mSSQueueStatus;
    private TextView mSSUpNext;
    private String mDisplayDuration;

    private DisplayMetrics mMetrics;

    private TextView mArtistName;
    private TextView mSongTitle;
    private TextView mAlbumTitle;
    private TextView mCurrentNdx;
    private ImageView mPoster;
    private ProgressBar mCurrentProgress;
    private TextView mCurrentPos;
    private TextView mRemainingTime;
    private int mCurrentDuration;
    private RowsSupportFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private static PositionableListRowPresenter mAudioQueuePresenter;

    private AudioNowPlayingActivity mActivity;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;
    public static int BACKDROP_ROTATION_INTERVAL = 10000;
    private static int NOWPLAYING_UI_REFRESH_INTERVAL = 15000;

    private BaseItemDto mBaseItem;
    private ListRow mQueueRow;

    private long lastUserInteraction;
    private long lastUIRefresh;
    private boolean shouldRefreshQueue = false;
    private boolean playerWasRecentlyNull = false;
    private boolean ssActive;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

    private PopupMenu mPopupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_now_playing);

        lastUserInteraction = System.currentTimeMillis();

        mActivity = this;

        mClock = findViewById(R.id.clock);
        mPoster = findViewById(R.id.poster);
        mArtistName = findViewById(R.id.artistTitle);
        mGenreRow = findViewById(R.id.genreRow);
        mSongTitle = findViewById(R.id.song);
        mAlbumTitle = findViewById(R.id.album);
        mCurrentNdx = findViewById(R.id.track);
        mScrollView = findViewById(R.id.mainScroller);
        mCounter = findViewById(R.id.counter);
        mLogoImage = findViewById(R.id.artistLogo);

        mSSArea = findViewById(R.id.ssInfoArea);
        mSSTime = findViewById(R.id.ssTime);
        mSSAlbumSong = findViewById(R.id.ssAlbumSong);
        mSSQueueStatus = findViewById(R.id.ssQueueStatus);
        mSSUpNext = findViewById(R.id.ssUpNext);

        mPlayPauseButton = findViewById(R.id.playPauseBtn);
        mPlayPauseButton.setContentDescription(getString(R.string.lbl_pause));
        mPlayPauseButton.setOnFocusChangeListener(mainAreaFocusListener);
        mPrevButton = findViewById(R.id.prevBtn);
        mPrevButton.setContentDescription(getString(R.string.lbl_prev_item));
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    mediaManager.getValue().prevAudioItem();
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });
        mPrevButton.setOnFocusChangeListener(mainAreaFocusListener);
        mNextButton = findViewById(R.id.nextBtn);
        mNextButton.setContentDescription(getString(R.string.lbl_next_item));
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    mediaManager.getValue().nextAudioItem();
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });
        mNextButton.setOnFocusChangeListener(mainAreaFocusListener);
        mRepeatButton = findViewById(R.id.repeatBtn);
        mRepeatButton.setContentDescription(getString(R.string.lbl_repeat));
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    mediaManager.getValue().toggleRepeat();
                    updateButtons(mediaManager.getValue().isPlayingAudio());
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });
        mSaveButton = findViewById(R.id.saveBtn);
        mSaveButton.setContentDescription(getString(R.string.lbl_save_as_playlist));
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    mediaManager.getValue().saveAudioQueue(mActivity);
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });
        mRepeatButton.setOnFocusChangeListener(mainAreaFocusListener);
        mShuffleButton = findViewById(R.id.shuffleBtn);
        mShuffleButton.setContentDescription(getString(R.string.lbl_reshuffle_queue));
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    mediaManager.getValue().shuffleAudioQueue();
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });
        mShuffleButton.setOnFocusChangeListener(mainAreaFocusListener);
        mAlbumButton = findViewById(R.id.albumBtn);
        mAlbumButton.setContentDescription(getString(R.string.lbl_open_album));
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                    lastUserInteraction = System.currentTimeMillis();
                } else {
                    Intent album = new Intent(mActivity, ItemListActivity.class);
                    album.putExtra("ItemId", mBaseItem.getAlbumId());
                    mActivity.startActivity(album);
                }
            }
        });
        mAlbumButton.setOnFocusChangeListener(mainAreaFocusListener);
        mArtistButton = findViewById(R.id.artistBtn);
        mArtistButton.setContentDescription(getString(R.string.lbl_open_artist));
        mArtistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                    lastUserInteraction = System.currentTimeMillis();
                } else if (mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0) {
                    Intent artist = new Intent(mActivity, FullDetailsActivity.class);
                    artist.putExtra("ItemId", mBaseItem.getAlbumArtists().get(0).getId());
                    mActivity.startActivity(artist);
                }
            }
        });
        mArtistButton.setOnFocusChangeListener(mainAreaFocusListener);

        mCurrentProgress = findViewById(R.id.playerProgress);
        mCurrentPos = findViewById(R.id.currentPos);
        mRemainingTime = findViewById(R.id.remainingTime);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssActive) {
                    stopScreenSaver();
                } else {
                    if (mediaManager.getValue().isPlayingAudio())
                        mediaManager.getValue().pauseAudio();
                    else mediaManager.getValue().resumeAudio();
                }
                lastUserInteraction = System.currentTimeMillis();
            }
        });

        backgroundService.getValue().attach(this);
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsSupportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mAudioQueuePresenter = new PositionableListRowPresenter(10);
        mRowsAdapter = new ArrayObjectAdapter(mAudioQueuePresenter);
        mRowsFragment.setAdapter(mRowsAdapter);
        addQueue();

        mPlayPauseButton.requestFocus();
    }

    protected void addQueue() {
        mQueueRow = new ListRow(new HeaderItem("Current Queue"), mediaManager.getValue().getCurrentAudioQueue());
        mediaManager.getValue().getCurrentAudioQueue().setRow(mQueueRow);
        mRowsAdapter.add(mQueueRow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItem();
        rotateBackdrops();
        if (!mediaManager.getValue().getIsAudioInitialized()) {
            Timber.d("audio player not initialized - setting buttons to state: not playing");
            updateButtons(false);
            playerWasRecentlyNull = true;
        } else {
            // refresh as soon as the audioEventListener is active
            queueNowplayingUIUpdate(500,true);
        }
        //link events
        mediaManager.getValue().addAudioEventListener(audioEventListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissPopup();
        mPoster.setKeepScreenOn(false);
        mediaManager.getValue().removeAudioEventListener(audioEventListener);
        stopRotate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotate();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        lastUserInteraction = System.currentTimeMillis();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (mediaManager.getValue().isPlayingAudio()) mediaManager.getValue().pauseAudio();
                else mediaManager.getValue().resumeAudio();
                if (ssActive) {
                    stopScreenSaver();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                mediaManager.getValue().nextAudioItem();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                mediaManager.getValue().prevAudioItem();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ssActive) {
                    mediaManager.getValue().nextAudioItem();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ssActive) {
                    mediaManager.getValue().prevAudioItem();
                    return true;
                }
                break;
        }

        if (ssActive) {
            stopScreenSaver();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private AudioEventListener audioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
            Timber.d("**** Got playstate change: %s", newState.toString());
            if (newState == PlaybackController.PlaybackState.PLAYING) {
                if (currentItem != mBaseItem || playerWasRecentlyNull) {
                    // new item started
                    if (currentItem != mBaseItem) loadItem();
                    playerWasRecentlyNull = false;
                    // immediately move the queue row to the current song
                    // disable queue refresh in onProgress since the queue position is already set
                    if (mAudioQueuePresenter != null && mediaManager.getValue().hasAudioQueueItems()) {
                        mAudioQueuePresenter.setPosition(mediaManager.getValue().getCurrentAudioQueuePosition());
                        shouldRefreshQueue = false;
                    }
                }
                // if audio player was recently null then playbackStateChange may be fired before all the button states are available
                if (ssActive) {
                    queueNowplayingUIUpdate(500, true);
                } else {
                    updateButtons(true);
                }
            } else if (newState == PlaybackController.PlaybackState.PAUSED || newState == PlaybackController.PlaybackState.IDLE) {
                // skip update since button handler will trigger it
                if (!ssActive) updateButtons(false);
                if (newState == PlaybackController.PlaybackState.IDLE && !mediaManager.getValue().hasNextAudioItem())
                    stopScreenSaver();
            }
        }

        @Override
        public void onProgress(long pos) {
            setCurrentTime(pos);
            if (mediaManager != null) {
                if (System.currentTimeMillis() - lastUIRefresh > NOWPLAYING_UI_REFRESH_INTERVAL) {
                    updateButtons(mediaManager.getValue().isPlayingAudio());
                    if (shouldRefreshQueue && mediaManager.getValue().hasAudioQueueItems()) {
                        mAudioQueuePresenter.setPosition(mediaManager.getValue().getCurrentAudioQueuePosition());
                        // start screensaver calls updateSSInfo so only subsequent items need this
                        if (ssActive) updateSSInfo();
                        shouldRefreshQueue = false;
                    }
                }
            }
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            if (hasQueue) {
                loadItem();
            } else {
                finish(); // entire queue removed nothing to do here
            }
        }

        @Override
        public void onQueueReplaced() {
            dismissPopup();
            mRowsAdapter.remove(mQueueRow);
            addQueue();
        }
    };

    private View.OnFocusChangeListener mainAreaFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) return;

            //scroll so entire main area is in view
            mScrollView.smoothScrollTo(0, 0);
            if (mediaManager.getValue().hasAudioQueueItems()) {
                //also re-position queue to current in case they scrolled around
                mAudioQueuePresenter.setPosition(mediaManager.getValue().getCurrentAudioQueuePosition());
            }
        }
    };

    private void updatePoster() {
        if (isFinishing()) return;
        // Figure image size
        Double aspect = ImageUtils.getImageAspectRatio(mBaseItem, false);
        int posterHeight = aspect > 1 ? Utils.convertDpToPixel(mActivity, 150) : Utils.convertDpToPixel(mActivity, 250);
        int posterWidth = (int) ((aspect) * posterHeight);
        if (posterHeight < 10)
            posterWidth = Utils.convertDpToPixel(mActivity, 150);  //Guard against zero size images causing picasso to barf

        String primaryImageUrl = ImageUtils.getPrimaryImageUrl(this, mBaseItem, apiClient.getValue(), false, posterHeight);
        Timber.d("Audio Poster url: %s", primaryImageUrl);
        Glide.with(mActivity)
                .load(primaryImageUrl)
                .error(R.drawable.ic_album)
                .override(posterWidth, posterHeight)
                .centerInside()
                .into(mPoster);
    }

    private void loadItem() {
        dismissPopup();
        mBaseItem = mediaManager.getValue().getCurrentAudioItem();
        if (mBaseItem != null) {
            updatePoster();
            updateInfo(mBaseItem);
            mDisplayDuration = TimeUtils.formatMillis((mBaseItem.getRunTimeTicks() != null ? mBaseItem.getRunTimeTicks() : 0) / 10000);
            shouldRefreshQueue = true;
        }
    }

    private void queueNowplayingUIUpdate(int delay, boolean refreshQueue) {
        shouldRefreshQueue = refreshQueue;
        lastUIRefresh = System.currentTimeMillis() - NOWPLAYING_UI_REFRESH_INTERVAL + delay;
    }

    private void updateButtons(final boolean playing) {
        lastUIRefresh = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPoster.setKeepScreenOn(playing);
                if (!playing) {
                    mPlayPauseButton.setImageResource(R.drawable.ic_play);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_play));
                } else {
                    mPlayPauseButton.setImageResource(R.drawable.ic_pause);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_pause));
                }
                mRepeatButton.setActivated(mediaManager.getValue().isRepeatMode());
                mSaveButton.setEnabled(mediaManager.getValue().getCurrentAudioQueueSize() > 1);
                mPrevButton.setEnabled(mediaManager.getValue().hasPrevAudioItem());
                mNextButton.setEnabled(mediaManager.getValue().hasNextAudioItem());
                mShuffleButton.setEnabled(mediaManager.getValue().getCurrentAudioQueueSize() > 1);
                if (mBaseItem != null) {
                    mAlbumButton.setEnabled(mBaseItem.getAlbumId() != null);
                    mArtistButton.setEnabled(mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0);
                }
            }
        });
    }

    private String getArtistName(BaseItemDto item) {
        return item.getArtists() != null && item.getArtists().size() > 0 ? item.getArtists().get(0) : item.getAlbumArtist();
    }

    private void updateInfo(BaseItemDto item) {
        if (item != null) {
            mArtistName.setText(getArtistName(item));
            mSongTitle.setText(item.getName());
            mAlbumTitle.setText(getResources().getString(R.string.lbl_now_playing_album, item.getAlbum()));
            mCurrentNdx.setText(getResources().getString(R.string.lbl_now_playing_track, mediaManager.getValue().getCurrentAudioQueueDisplayPosition(), mediaManager.getValue().getCurrentAudioQueueDisplaySize()));
            mCurrentDuration = ((Long) ((item.getRunTimeTicks() != null ? item.getRunTimeTicks() : 0) / 10000)).intValue();
            //set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            addGenres(mGenreRow);
            backgroundService.getValue().setBackground(item);
        }
    }

    public void setCurrentTime(long time) {
        if (ssActive) {
            mSSTime.setText(TimeUtils.formatMillis(time) + " / " + mDisplayDuration);
        } else {
            mCurrentProgress.setProgress(((Long) time).intValue());
            mCurrentPos.setText(TimeUtils.formatMillis(time));
            mRemainingTime.setText(mCurrentDuration > 0 ? "-" + TimeUtils.formatMillis(mCurrentDuration - time) : "");
        }
    }

    private void addGenres(TextView textView) {
        textView.setText(TextUtils.join(" / ", mBaseItem.getGenres()));
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            lastUserInteraction = System.currentTimeMillis();
            if (ssActive) {
                stopScreenSaver();
            } else {
                mPopupMenu =  KeyProcessor.createItemMenu((BaseRowItem) item, ((BaseRowItem) item).getBaseItem().getUserData(), mActivity);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof BaseRowItem) {
                //Keep counter
                mCounter.setText(((BaseRowItem) item).getIndex() + 1 + " | " + mQueueRow.getAdapter().size());
            }
        }
    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                if (mBaseItem != null && (mBaseItem.getBackdropCount() > 1 || (mBaseItem.getParentBackdropImageTags() != null && mBaseItem.getParentBackdropImageTags().size() > 1)))
                    backgroundService.getValue().setBackground(mBaseItem);

                //manage our "screen saver" too
                if (mediaManager.getValue().isPlayingAudio() && !ssActive && System.currentTimeMillis() - lastUserInteraction > 60000) {
                    startScreenSaver();
                }

                mLoopHandler.postDelayed(this, BACKDROP_ROTATION_INTERVAL);
            }
        };

        mLoopHandler.postDelayed(mBackdropLoop, BACKDROP_ROTATION_INTERVAL);
    }

    private void stopRotate() {
        if (mLoopHandler != null && mBackdropLoop != null) {
            mLoopHandler.removeCallbacks(mBackdropLoop);
        }
    }

    private void dismissPopup() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }
    }

    protected void startScreenSaver() {
        if (ssActive) return;
        dismissPopup();
        // update ss info since current item may not have ss info if screensaver opens too soon
        updateSSInfo();
        mArtistName.setAlpha(.3f);
        mGenreRow.setVisibility(View.INVISIBLE);
        mClock.setAlpha(.3f);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mScrollView, "alpha", 1f, 0f);
        fadeOut.setDuration(1000);
        fadeOut.start();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mSSArea, "alpha", 0f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.start();

        ssActive = true;
        setCurrentTime(mediaManager.getValue().getCurrentAudioPosition());
    }

    protected void stopScreenSaver() {
        if (!ssActive) return;
        if (mediaManager.getValue().hasAudioQueueItems()) {
            mPlayPauseButton.requestFocus();
        }
        mLogoImage.setVisibility(View.GONE);
        mArtistName.setAlpha(1f);
        mGenreRow.setVisibility(View.VISIBLE);
        mClock.setAlpha(1f);
        setCurrentTime(mediaManager.getValue().getCurrentAudioPosition());
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mSSArea, "alpha", 1f, 0f);
        fadeOut.setDuration(1000);
        fadeOut.start();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mScrollView, "alpha", 0f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.start();
        lastUserInteraction = System.currentTimeMillis();
        ssActive = false;
    }

    protected void updateSSInfo() {
        mSSAlbumSong.setText((mBaseItem.getAlbum() != null ? mBaseItem.getAlbum() + " / " : "") + mBaseItem.getName());
        mSSQueueStatus.setText(mediaManager.getValue().getCurrentAudioQueueDisplayPosition() + " | " + mediaManager.getValue().getCurrentAudioQueueDisplaySize());
        BaseItemDto next = mediaManager.getValue().getNextAudioItem();
        mSSUpNext.setText(next != null ? getString(R.string.lbl_up_next_colon) + "  " + (getArtistName(next) != null ? getArtistName(next) + " / " : "") + next.getName() : "");
    }
}
