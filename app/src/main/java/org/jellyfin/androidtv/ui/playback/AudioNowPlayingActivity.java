package org.jellyfin.androidtv.ui.playback;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.ui.ClockUserView;
import org.jellyfin.androidtv.ui.GenreButton;
import org.jellyfin.androidtv.ui.ImageButton;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import kotlin.Lazy;
import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.inject;

public class AudioNowPlayingActivity extends BaseActivity {
    private LinearLayout mGenreRow;
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

    private BaseItemDto mBaseItem;
    private ListRow mQueueRow;
    private boolean mApplyAlpha = true;

    private long lastUserInteraction;
    private boolean ssActive;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);
    private Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private Lazy<MediaManager> mediaManager = inject(MediaManager.class);

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
        mPlayPauseButton.setSecondaryImage(R.drawable.ic_pause);
        mPlayPauseButton.setPrimaryImage(R.drawable.ic_play);
        TextView helpView = findViewById(R.id.buttonTip);
        mPrevButton = findViewById(R.id.prevBtn);
        mPrevButton.setContentDescription(getString(R.string.lbl_prev_item));
        mPrevButton.setHelpView(helpView);
        mPrevButton.setHelpText(getString(R.string.lbl_prev_item));
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().prevAudioItem();
            }
        });
        mPrevButton.setGotFocusListener(mainAreaFocusListener);
        mNextButton = findViewById(R.id.nextBtn);
        mNextButton.setContentDescription(getString(R.string.lbl_next_item));
        mNextButton.setHelpView(helpView);
        mNextButton.setHelpText(getString(R.string.lbl_next_item));
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().nextAudioItem();
            }
        });
        mNextButton.setGotFocusListener(mainAreaFocusListener);
        mRepeatButton = findViewById(R.id.repeatBtn);
        mRepeatButton.setContentDescription(getString(R.string.lbl_repeat));
        mRepeatButton.setHelpView(helpView);
        mRepeatButton.setHelpText(getString(R.string.lbl_repeat));
        mRepeatButton.setPrimaryImage(R.drawable.ic_loop);
        mRepeatButton.setSecondaryImage(R.drawable.ic_loop_red);
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().toggleRepeat();
                updateButtons(mediaManager.getValue().isPlayingAudio());
            }
        });
        mSaveButton = findViewById(R.id.saveBtn);
        mSaveButton.setContentDescription(getString(R.string.lbl_save_as_playlist));
        mSaveButton.setHelpView(helpView);
        mSaveButton.setHelpText(getString(R.string.lbl_save_as_playlist));
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaManager.getValue().saveAudioQueue(mActivity);
            }
        });
        mRepeatButton.setGotFocusListener(mainAreaFocusListener);
        mShuffleButton = findViewById(R.id.shuffleBtn);
        mShuffleButton.setContentDescription(getString(R.string.lbl_reshuffle_queue));
        mShuffleButton.setHelpView(helpView);
        mShuffleButton.setHelpText(getString(R.string.lbl_reshuffle_queue));
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mediaManager.getValue().shuffleAudioQueue();
            }
        });
        mShuffleButton.setGotFocusListener(mainAreaFocusListener);
        mAlbumButton = findViewById(R.id.albumBtn);
        mAlbumButton.setContentDescription(getString(R.string.lbl_open_album));
        mAlbumButton.setHelpView(helpView);
        mAlbumButton.setHelpText(getString(R.string.lbl_open_album));
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent album = new Intent(mActivity, ItemListActivity.class);
                album.putExtra("ItemId", mBaseItem.getAlbumId());
                mActivity.startActivity(album);
            }
        });
        mAlbumButton.setGotFocusListener(mainAreaFocusListener);
        mArtistButton = findViewById(R.id.artistBtn);
        mArtistButton.setContentDescription(getString(R.string.lbl_open_artist));
        mArtistButton.setHelpView(helpView);
        mArtistButton.setHelpText(getString(R.string.lbl_open_artist));
        mArtistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0) {
                    Intent artist = new Intent(mActivity, FullDetailsActivity.class);
                    artist.putExtra("ItemId", mBaseItem.getAlbumArtists().get(0).getId());
                    mActivity.startActivity(artist);

                }
            }
        });
        mArtistButton.setGotFocusListener(mainAreaFocusListener);

        mCurrentProgress = findViewById(R.id.playerProgress);
        mCurrentPos = findViewById(R.id.currentPos);
        mRemainingTime = findViewById(R.id.remainingTime);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaManager.getValue().isPlayingAudio()) mediaManager.getValue().pauseAudio();
                else mediaManager.getValue().resumeAudio();
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
        if (mBaseItem != null && (mBaseItem.getBackdropCount() > 1 || (mBaseItem.getParentBackdropImageTags() != null && mBaseItem.getParentBackdropImageTags().size() > 1)))
            rotateBackdrops();
        //link events
        mediaManager.getValue().addAudioEventListener(audioEventListener);
        //Make sure our initial button state reflects playback properly accounting for late loading of the audio stream
        mLoopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateButtons(mediaManager.getValue().isPlayingAudio());
            }
        }, 750);
    }

    @Override
    protected void onPause() {
        super.onPause();
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
            if (newState == PlaybackController.PlaybackState.PLAYING && currentItem != mBaseItem) {
                // new item started
                loadItem();
                updateButtons(true);
                mAudioQueuePresenter.setPosition(mediaManager.getValue().getCurrentAudioQueuePosition());
            } else {
                updateButtons(newState == PlaybackController.PlaybackState.PLAYING);
                if (newState == PlaybackController.PlaybackState.IDLE && !mediaManager.getValue().hasNextAudioItem())
                    stopScreenSaver();
            }
        }

        @Override
        public void onProgress(long pos) {
            setCurrentTime(pos);
        }

        @Override
        public void onQueueStatusChanged(boolean hasQueue) {
            if (hasQueue) {
                loadItem();
                updateButtons(mediaManager.getValue().isPlayingAudio());
            } else {
                finish(); // entire queue removed nothing to do here
            }
        }

        @Override
        public void onQueueReplaced() {
            mRowsAdapter.remove(mQueueRow);
            addQueue();
        }
    };

    private GotFocusEvent mainAreaFocusListener = new GotFocusEvent() {
        @Override
        public void gotFocus(View v) {
            //scroll so entire main area is in view
            mScrollView.smoothScrollTo(0, 0);
            //also re-position queue to current in case they scrolled around
            mAudioQueuePresenter.setPosition(mediaManager.getValue().getCurrentAudioQueuePosition());
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
        mBaseItem = mediaManager.getValue().getCurrentAudioItem();
        if (mBaseItem != null) {
            updatePoster();
            updateInfo(mBaseItem);
            mDisplayDuration = TimeUtils.formatMillis((mBaseItem.getRunTimeTicks() != null ? mBaseItem.getRunTimeTicks() : 0) / 10000);
            // give audio a chance to start playing before updating next info
            mLoopHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateSSInfo();
                }
            }, 750);
        }
    }

    private void updateButtons(final boolean playing) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPoster.setKeepScreenOn(playing);
                if (!playing) {
                    mPlayPauseButton.setState(ImageButton.STATE_PRIMARY);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_play));
                } else {
                    mPlayPauseButton.setState(ImageButton.STATE_SECONDARY);
                    mPlayPauseButton.setContentDescription(getString(R.string.lbl_pause));
                }
                mRepeatButton.setState(mediaManager.getValue().isRepeatMode() ? ImageButton.STATE_SECONDARY : ImageButton.STATE_PRIMARY);
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

    private void addGenres(LinearLayout layout) {
        layout.removeAllViews();
        if (mBaseItem.getGenres() != null && mBaseItem.getGenres().size() > 0) {
            boolean first = true;
            for (String genre : mBaseItem.getGenres()) {
                if (!first) InfoLayoutHelper.addSpacer(this, layout, "  /  ", 14);
                first = false;
                layout.addView(new GenreButton(this, 16, genre, mBaseItem.getBaseItemType()));
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, (BaseRowItem) item, mActivity);
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

    protected void startScreenSaver() {
        mArtistName.setAlpha(.3f);
        mGenreRow.setVisibility(View.INVISIBLE);
        mClock.setAlpha(.3f);
        mApplyAlpha = false;
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
        mApplyAlpha = true;
        mLogoImage.setVisibility(View.GONE);
        mSSArea.setAlpha(0f);
        mArtistName.setAlpha(1f);
        mGenreRow.setVisibility(View.VISIBLE);
        mClock.setAlpha(1f);
        mScrollView.setAlpha(1f);
        ssActive = false;
        setCurrentTime(mediaManager.getValue().getCurrentAudioPosition());

    }

    protected void updateSSInfo() {
        mSSAlbumSong.setText((mBaseItem.getAlbum() != null ? mBaseItem.getAlbum() + " / " : "") + mBaseItem.getName());
        mSSQueueStatus.setText(mediaManager.getValue().getCurrentAudioQueueDisplayPosition() + " | " + mediaManager.getValue().getCurrentAudioQueueDisplaySize());
        BaseItemDto next = mediaManager.getValue().getNextAudioItem();
        mSSUpNext.setText(next != null ? getString(R.string.lbl_up_next_colon) + "  " + (getArtistName(next) != null ? getArtistName(next) + " / " : "") + next.getName() : "");
    }
}
