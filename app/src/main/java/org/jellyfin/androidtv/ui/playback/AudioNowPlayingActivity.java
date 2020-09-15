package org.jellyfin.androidtv.ui.playback;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import androidx.leanback.app.BackgroundManager;
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
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.model.GotFocusEvent;
import org.jellyfin.androidtv.ui.ClockUserView;
import org.jellyfin.androidtv.ui.GenreButton;
import org.jellyfin.androidtv.ui.ImageButton;
import org.jellyfin.androidtv.ui.shared.BaseActivity;
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity;
import org.jellyfin.androidtv.ui.itemdetail.ItemListActivity;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.BackgroundManagerExtensionsKt;
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

    private int BUTTON_SIZE;

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

    private long lastUserInteraction;
    private boolean ssActive;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_now_playing);

        lastUserInteraction = System.currentTimeMillis();

        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);
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
        mPlayPauseButton.setSecondaryImage(R.drawable.ic_pause);
        mPlayPauseButton.setPrimaryImage(R.drawable.ic_play);
        TextView helpView = findViewById(R.id.buttonTip);
        mPrevButton = findViewById(R.id.prevBtn);
        mPrevButton.setHelpView(helpView);
        mPrevButton.setHelpText(getString(R.string.lbl_prev_item));
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.prevAudioItem();
            }
        });
        mPrevButton.setGotFocusListener(mainAreaFocusListener);
        mNextButton = findViewById(R.id.nextBtn);
        mNextButton.setHelpView(helpView);
        mNextButton.setHelpText(getString(R.string.lbl_next_item));
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.nextAudioItem();
            }
        });
        mNextButton.setGotFocusListener(mainAreaFocusListener);
        mRepeatButton = findViewById(R.id.repeatBtn);
        mRepeatButton.setHelpView(helpView);
        mRepeatButton.setHelpText(getString(R.string.lbl_repeat));
        mRepeatButton.setPrimaryImage(R.drawable.ic_loop);
        mRepeatButton.setSecondaryImage(R.drawable.ic_loop_red);
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.toggleRepeat();
                updateButtons(MediaManager.isPlayingAudio());
            }
        });
        mSaveButton = findViewById(R.id.saveBtn);
        mSaveButton.setHelpView(helpView);
        mSaveButton.setHelpText(getString(R.string.lbl_save_as_playlist));
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.saveAudioQueue(mActivity);
            }
        });
        mRepeatButton.setGotFocusListener(mainAreaFocusListener);
        mShuffleButton = findViewById(R.id.shuffleBtn);
        mShuffleButton.setHelpView(helpView);
        mShuffleButton.setHelpText(getString(R.string.lbl_reshuffle_queue));
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.lbl_shuffle)
                        .setMessage(R.string.msg_reshuffle_audio_queue)
                        .setPositiveButton(mActivity.getString(R.string.lbl_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MediaManager.shuffleAudioQueue();
                            }
                        })
                        .setNegativeButton(mActivity.getString(R.string.lbl_no), null)
                        .show();
            }
        });
        mShuffleButton.setGotFocusListener(mainAreaFocusListener);
        mAlbumButton = findViewById(R.id.albumBtn);
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
                if (MediaManager.isPlayingAudio()) MediaManager.pauseAudio();
                else MediaManager.resumeAudio();
            }
        });

        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsSupportFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mAudioQueuePresenter = new PositionableListRowPresenter(getDrawable(R.color.black_transparent_light), 10);
        mRowsAdapter = new ArrayObjectAdapter(mAudioQueuePresenter);
        mRowsFragment.setAdapter(mRowsAdapter);
        addQueue();

        mPlayPauseButton.requestFocus();
    }

    protected void addQueue() {
        mQueueRow = new ListRow(new HeaderItem("Current Queue"), MediaManager.getCurrentAudioQueue());
        MediaManager.getCurrentAudioQueue().setRow(mQueueRow);
        mRowsAdapter.add(mQueueRow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItem();
        rotateBackdrops();
        //link events
        MediaManager.addAudioEventListener(audioEventListener);
        //Make sure our initial button state reflects playback properly accounting for late loading of the audio stream
        mLoopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateButtons(MediaManager.isPlayingAudio());
            }
        }, 750);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPoster.setKeepScreenOn(false);
        MediaManager.removeAudioEventListener(audioEventListener);
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
                if (MediaManager.isPlayingAudio()) MediaManager.pauseAudio();
                else MediaManager.resumeAudio();
                if (ssActive) {
                    stopScreenSaver();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                MediaManager.nextAudioItem();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                MediaManager.prevAudioItem();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ssActive) {
                    MediaManager.nextAudioItem();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ssActive) {
                    MediaManager.prevAudioItem();
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
                mAudioQueuePresenter.setPosition(MediaManager.getCurrentAudioQueuePosition());
            } else {
                updateButtons(newState == PlaybackController.PlaybackState.PLAYING);
                if (newState == PlaybackController.PlaybackState.IDLE && !MediaManager.hasNextAudioItem())
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
                updateButtons(MediaManager.isPlayingAudio());
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
            mAudioQueuePresenter.setPosition(MediaManager.getCurrentAudioQueuePosition());
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

        String primaryImageUrl = ImageUtils.getPrimaryImageUrl(mBaseItem, apiClient.getValue(), false, posterHeight);
        Timber.d("Audio Poster url: %s", primaryImageUrl);
        Glide.with(mActivity)
                .load(primaryImageUrl)
                .error(R.drawable.ic_album)
                .override(posterWidth, posterHeight)
                .centerInside()
                .into(mPoster);
    }

    private void loadItem() {
        mBaseItem = MediaManager.getCurrentAudioItem();
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
                mPlayPauseButton.setState(!playing ? ImageButton.STATE_PRIMARY : ImageButton.STATE_SECONDARY);
                mRepeatButton.setState(MediaManager.isRepeatMode() ? ImageButton.STATE_SECONDARY : ImageButton.STATE_PRIMARY);
                mSaveButton.setEnabled(MediaManager.getCurrentAudioQueueSize() > 1);
                mPrevButton.setEnabled(MediaManager.hasPrevAudioItem());
                mNextButton.setEnabled(MediaManager.hasNextAudioItem());
                mShuffleButton.setEnabled(MediaManager.getCurrentAudioQueueSize() > 1);
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
            mCurrentNdx.setText(getResources().getString(R.string.lbl_now_playing_track, MediaManager.getCurrentAudioQueueDisplayPosition(), MediaManager.getCurrentAudioQueueDisplaySize()));
            mCurrentDuration = ((Long) ((item.getRunTimeTicks() != null ? item.getRunTimeTicks() : 0) / 10000)).intValue();
            //set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            addGenres(mGenreRow);
            updateBackground(ImageUtils.getBackdropImageUrl(item, apiClient.getValue(), true));
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
                updateBackground(ImageUtils.getBackdropImageUrl(mBaseItem, apiClient.getValue(), true));
                //manage our "screen saver" too
                if (MediaManager.isPlayingAudio() && !ssActive && System.currentTimeMillis() - lastUserInteraction > 60000) {
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
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mScrollView, "alpha", 1f, 0f);
        fadeOut.setDuration(1000);
        fadeOut.start();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mSSArea, "alpha", 0f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.start();

        ssActive = true;
        setCurrentTime(MediaManager.getCurrentAudioPosition());
    }

    protected void stopScreenSaver() {
        mLogoImage.setVisibility(View.GONE);
        mSSArea.setAlpha(0f);
        mArtistName.setAlpha(1f);
        mGenreRow.setVisibility(View.VISIBLE);
        mClock.setAlpha(1f);
        mScrollView.setAlpha(1f);
        ssActive = false;
        setCurrentTime(MediaManager.getCurrentAudioPosition());

    }

    protected void updateSSInfo() {
        mSSAlbumSong.setText((mBaseItem.getAlbum() != null ? mBaseItem.getAlbum() + " / " : "") + mBaseItem.getName());
        mSSQueueStatus.setText(MediaManager.getCurrentAudioQueueDisplayPosition() + " | " + MediaManager.getCurrentAudioQueueDisplaySize());
        BaseItemDto next = MediaManager.getNextAudioItem();
        mSSUpNext.setText(next != null ? getString(R.string.lbl_up_next_colon) + "  " + (getArtistName(next) != null ? getArtistName(next) + " / " : "") + next.getName() : "");
    }

    protected void updateLogo() {
        if (mBaseItem.getHasLogo() || mBaseItem.getParentLogoImageTag() != null) {
            if (ssActive) {
                mLogoImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(ImageUtils.getLogoImageUrl(mBaseItem, apiClient.getValue()))
                        .override(700, 200)
                        .centerInside()
                        .into(mLogoImage);
                mArtistName.setVisibility(View.INVISIBLE);
            }
        } else {
            mLogoImage.setVisibility(View.GONE);
            mArtistName.setVisibility(View.VISIBLE);
        }
    }

    protected void updateBackground(String url) {
        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        if (url == null) {
            backgroundManager.setDrawable(null);
        } else {
            BackgroundManagerExtensionsKt.drawable(
                    backgroundManager,
                    this,
                    url,
                    mMetrics.widthPixels,
                    mMetrics.heightPixels
            );
        }
    }
}
