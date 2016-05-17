package tv.emby.embyatv.playback;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.details.ItemListActivity;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.model.GotFocusEvent;
import tv.emby.embyatv.presentation.PositionableListRowPresenter;
import tv.emby.embyatv.ui.ClockUserView;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/19/2015.
 */
public class AudioNowPlayingActivity extends BaseActivity  {

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

    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    private TextView mArtistName;
    private TextView mSongTitle;
    private TextView mAlbumTitle;
    private TextView mCurrentNdx;
    private TextView mTotal;
    private ImageView mPoster;
    private ProgressBar mCurrentProgress;
    private TextView mCurrentPos;
    private TextView mRemainingTime;
    private int mCurrentDuration;
    private RowsFragment mRowsFragment;
    private ArrayObjectAdapter mRowsAdapter;
    private static PositionableListRowPresenter mAudioQueuePresenter;

    private TvApp mApplication;
    private AudioNowPlayingActivity mActivity;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;
    public static int BACKDROP_ROTATION_INTERVAL = 10000;
    private Typeface roboto;

    private BaseItemDto mBaseItem;
    private ListRow mQueueRow;

    private long lastUserInteraction;
    private boolean ssActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_now_playing);

        lastUserInteraction = System.currentTimeMillis();

        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);
        mApplication = TvApp.getApplication();
        mActivity = this;
        roboto = mApplication.getDefaultFont();

        mClock = (ClockUserView) findViewById(R.id.clock);
        mPoster = (ImageView) findViewById(R.id.poster);
        mArtistName = (TextView) findViewById(R.id.artistTitle);
        mArtistName.setTypeface(roboto);
        mGenreRow = (LinearLayout) findViewById(R.id.genreRow);
        mSongTitle = (TextView) findViewById(R.id.songTitle);
        mSongTitle.setTypeface(roboto);
        mAlbumTitle = (TextView) findViewById(R.id.albumTitle);
        mAlbumTitle.setTypeface(roboto);
        mCurrentNdx = (TextView) findViewById(R.id.currentNdx);
        mScrollView = (ScrollView) findViewById(R.id.mainScroller);
        mCounter = (TextView) findViewById(R.id.counter);
        mCounter.setTypeface(roboto);
        mLogoImage = (ImageView) findViewById(R.id.artistLogo);

        mSSArea = (RelativeLayout) findViewById(R.id.ssInfoArea);
        mSSTime = (TextView) findViewById(R.id.ssTime);
        mSSTime.setTypeface(roboto);
        mSSAlbumSong = (TextView) findViewById(R.id.ssAlbumSong);
        mSSAlbumSong.setTypeface(roboto);
        mSSQueueStatus = (TextView) findViewById(R.id.ssQueueStatus);
        mSSQueueStatus.setTypeface(roboto);
        mSSUpNext = (TextView) findViewById(R.id.ssUpNext);
        mSSUpNext.setTypeface(roboto);

        mPlayPauseButton = (ImageButton) findViewById(R.id.playPauseBtn);
        mPlayPauseButton.setSecondaryImage(R.drawable.lb_ic_pause);
        mPlayPauseButton.setPrimaryImage(R.drawable.play);
        TextView helpView = (TextView) findViewById(R.id.buttonTip);
        mPrevButton = (ImageButton) findViewById(R.id.prevBtn);
        mPrevButton.setHelpView(helpView);
        mPrevButton.setHelpText(getString(R.string.lbl_prev_item));
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.prevAudioItem();
            }
        });
        mPrevButton.setGotFocusListener(mainAreaFocusListener);
        mNextButton = (ImageButton) findViewById(R.id.nextBtn);
        mNextButton.setHelpView(helpView);
        mNextButton.setHelpText(getString(R.string.lbl_next_item));
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.nextAudioItem();
            }
        });
        mNextButton.setGotFocusListener(mainAreaFocusListener);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatBtn);
        mRepeatButton.setHelpView(helpView);
        mRepeatButton.setHelpText(getString(R.string.lbl_toggle_repeat));
        mRepeatButton.setPrimaryImage(R.drawable.loop);
        mRepeatButton.setSecondaryImage(R.drawable.loopred);
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.toggleRepeat();
                updateButtons(MediaManager.isPlayingAudio());
            }
        });
        mSaveButton = (ImageButton) findViewById(R.id.saveBtn);
        mSaveButton.setHelpView(helpView);
        mSaveButton.setHelpText(getString(R.string.lbl_save_as_playlist));
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.saveAudioQueue(mActivity);
            }
        });
        mRepeatButton.setGotFocusListener(mainAreaFocusListener);
        mShuffleButton = (ImageButton) findViewById(R.id.shuffleBtn);
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
                                mRowsAdapter.remove(mQueueRow);
                                addQueue();
                            }
                        })
                        .setNegativeButton(mActivity.getString(R.string.lbl_no), null)
                        .show();
            }
        });
        mShuffleButton.setGotFocusListener(mainAreaFocusListener);
        mAlbumButton = (ImageButton) findViewById(R.id.albumBtn);
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
        mArtistButton = (ImageButton) findViewById(R.id.artistBtn);
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

        mCurrentProgress = (ProgressBar) findViewById(R.id.playerProgress);
        mCurrentPos = (TextView) findViewById(R.id.currentPos);
        mRemainingTime = (TextView) findViewById(R.id.remainingTime);
        mTotal = (TextView) findViewById(R.id.total);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MediaManager.isPlayingAudio()) MediaManager.pauseAudio();
                else MediaManager.resumeAudio();
            }
        });

        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mRowsFragment = new RowsFragment();
        getFragmentManager().beginTransaction().add(R.id.rowsFragment, mRowsFragment).commit();

        mRowsFragment.setOnItemViewClickedListener(new ItemViewClickedListener());
        mRowsFragment.setOnItemViewSelectedListener(new ItemViewSelectedListener());
        mAudioQueuePresenter = new PositionableListRowPresenter();
        mRowsAdapter = new ArrayObjectAdapter(mAudioQueuePresenter);
        mRowsFragment.setAdapter(mRowsAdapter);
        addQueue();

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

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
                if (MediaManager.isPlayingAudio()) MediaManager.pauseAudio(); else MediaManager.resumeAudio();
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
            mApplication.getLogger().Debug("**** Got playstate change: " + newState);
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
        Double aspect = Utils.getImageAspectRatio(mBaseItem, false);
        int posterHeight = aspect > 1 ? Utils.convertDpToPixel(mActivity, 150) : Utils.convertDpToPixel(mActivity, 250);
        int posterWidth = (int) ((aspect) * posterHeight);
        if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(mActivity, 150);  //Guard against zero size images causing picasso to barf

        String primaryImageUrl = Utils.getPrimaryImageUrl(mBaseItem, mApplication.getApiClient(),false, false, posterHeight);
        mApplication.getLogger().Debug("Audio Poster url: " + primaryImageUrl);
        Picasso.with(mActivity)
                .load(primaryImageUrl)
                .skipMemoryCache()
                .error(R.drawable.audioicon)
                .resize(posterWidth, posterHeight)
                .centerInside()
                .into(mPoster);
    }

    private void loadItem() {
        mBaseItem = MediaManager.getCurrentAudioItem();
        if (mBaseItem != null) {
            updatePoster();
            updateInfo(mBaseItem);
            mDisplayDuration = Utils.formatMillis((mBaseItem.getRunTimeTicks() != null ? mBaseItem.getRunTimeTicks() : 0) / 10000);
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
            mAlbumTitle.setText(item.getAlbum());
            mCurrentNdx.setText(MediaManager.getCurrentAudioQueueDisplayPosition());
            mTotal.setText(MediaManager.getCurrentAudioQueueDisplaySize());
            mCurrentDuration = ((Long)((item.getRunTimeTicks() != null ? item.getRunTimeTicks() : 0) / 10000)).intValue();
            //set progress to match duration
            mCurrentProgress.setMax(mCurrentDuration);
            addGenres(mGenreRow);
            updateBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true));
        }

    }

    public void setCurrentTime(long time) {
        if (ssActive) {
            mSSTime.setText(Utils.formatMillis(time) + " / " + mDisplayDuration);
        } else {
            mCurrentProgress.setProgress(((Long) time).intValue());
            mCurrentPos.setText(Utils.formatMillis(time));
            mRemainingTime.setText(mCurrentDuration > 0 ? "-" + Utils.formatMillis(mCurrentDuration - time) : "");
        }
    }

    private void addGenres(LinearLayout layout) {
        layout.removeAllViews();
        if (mBaseItem.getGenres() != null && mBaseItem.getGenres().size() > 0) {
            boolean first = true;
            for (String genre : mBaseItem.getGenres()) {
                if (!first) InfoLayoutHelper.addSpacer(this, layout, "  /  ", 14);
                first = false;
                layout.addView(new GenreButton(this, roboto, 16, genre, mBaseItem.getType()));
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
                mCounter.setText(((BaseRowItem) item).getIndex()+1 + " | "+mQueueRow.getAdapter().size());
            }
        }
    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
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
                Picasso.with(this)
                        .load(Utils.getLogoImageUrl(mBaseItem, TvApp.getApplication().getApiClient()))
                        .resize(700, 200)
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
        if (url == null) {
            BackgroundManager.getInstance(this).setDrawable(mDefaultBackground);
        } else {
            Picasso.with(this)
                    .load(url)
                    .skipMemoryCache()
                    .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                    .centerCrop()
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
    }

}
