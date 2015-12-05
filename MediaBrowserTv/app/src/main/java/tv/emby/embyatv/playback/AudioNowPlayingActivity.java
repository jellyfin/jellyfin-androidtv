package tv.emby.embyatv.playback;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.details.SongListActivity;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.model.GotFocusEvent;
import tv.emby.embyatv.presentation.PositionableListRowPresenter;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.InfoLayoutHelper;
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
    private ScrollView mScrollView;

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
    public static int BACKDROP_ROTATION_INTERVAL = 8000;
    private Typeface roboto;

    private BaseItemDto mBaseItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_now_playing);

        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);
        mApplication = TvApp.getApplication();
        mActivity = this;
        roboto = mApplication.getDefaultFont();

        mPoster = (ImageView) findViewById(R.id.poster);
        mArtistName = (TextView) findViewById(R.id.artistTitle);
        mArtistName.setTypeface(roboto);
        mArtistName.setShadowLayer(5, 5, 5, Color.BLACK);
        mGenreRow = (LinearLayout) findViewById(R.id.genreRow);
        mSongTitle = (TextView) findViewById(R.id.songTitle);
        mSongTitle.setTypeface(roboto);
        mAlbumTitle = (TextView) findViewById(R.id.albumTitle);
        mAlbumTitle.setTypeface(roboto);
        mCurrentNdx = (TextView) findViewById(R.id.currentNdx);
        mScrollView = (ScrollView) findViewById(R.id.mainScroller);

        mPlayPauseButton = (ImageButton) findViewById(R.id.playPauseBtn);
        mPlayPauseButton.setSecondaryImage(R.drawable.lb_ic_pause);
        mPlayPauseButton.setPrimaryImage(R.drawable.play);
        TextView helpView = (TextView) findViewById(R.id.buttonTip);
        mPrevButton = (ImageButton) findViewById(R.id.prevBtn);
        mPrevButton.setHelpView(helpView);
        mPrevButton.setHelpText("Restart/Previous Item");
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.prevAudioItem();
            }
        });
        mPrevButton.setGotFocusListener(mainAreaFocusListener);
        mNextButton = (ImageButton) findViewById(R.id.nextBtn);
        mNextButton.setHelpView(helpView);
        mNextButton.setHelpText("Next Item");
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.nextAudioItem();
            }
        });
        mNextButton.setGotFocusListener(mainAreaFocusListener);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatBtn);
        mRepeatButton.setHelpView(helpView);
        mRepeatButton.setHelpText("Toggle Repeat");
        mRepeatButton.setPrimaryImage(R.drawable.loop);
        mRepeatButton.setSecondaryImage(R.drawable.loopred);
        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaManager.toggleRepeat();
                updateButtons(MediaManager.isPlayingAudio());
            }
        });
        mRepeatButton.setGotFocusListener(mainAreaFocusListener);
        mShuffleButton = (ImageButton) findViewById(R.id.shuffleBtn);
        mShuffleButton.setHelpView(helpView);
        mShuffleButton.setHelpText("Re-shuffle Queue");
        mShuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mActivity)
                        .setTitle("Shuffle")
                        .setMessage("Re-shuffle current audio queue?")
                        .setPositiveButton(mActivity.getString(R.string.lbl_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //todo shuffle
                            }
                        })
                        .setNegativeButton(mActivity.getString(R.string.lbl_no), null)
                        .show();
            }
        });
        mShuffleButton.setGotFocusListener(mainAreaFocusListener);
        mAlbumButton = (ImageButton) findViewById(R.id.albumBtn);
        mAlbumButton.setHelpView(helpView);
        mAlbumButton.setHelpText("Open Album");
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent album = new Intent(mActivity, SongListActivity.class);
                album.putExtra("ItemId", mBaseItem.getAlbumId());
                mActivity.startActivity(album);
            }
        });
        mAlbumButton.setGotFocusListener(mainAreaFocusListener);
        mArtistButton = (ImageButton) findViewById(R.id.artistBtn);
        mArtistButton.setHelpView(helpView);
        mArtistButton.setHelpText("Open Artist");
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

        ListRow queue = new ListRow(new HeaderItem("Current Queue",null), MediaManager.getCurrentAudioQueue());
        MediaManager.getCurrentAudioQueue().setRow(queue);
        mRowsAdapter.add(queue);

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        //link events
        MediaManager.addAudioEventListener(new IAudioEventListener() {
            @Override
            public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
                mApplication.getLogger().Debug("**** Got playstate change: "+newState);
                if (newState == PlaybackController.PlaybackState.PLAYING && currentItem != mBaseItem) {
                    // new item started
                    loadItem();
                    updateButtons(true);
                    mAudioQueuePresenter.setPosition(MediaManager.getCurrentAudioQueuePosition());
                } else {
                    updateButtons(newState == PlaybackController.PlaybackState.PLAYING);
                }
            }

            @Override
            public void onProgress(long pos) {
                setCurrentTime(pos);
            }
        });

        loadItem();

        mPlayPauseButton.requestFocus();

    }

    @Override
    protected void onResume() {
        super.onResume();
        rotateBackdrops();
        //Make sure our initial button state reflects playback properly accounting for late loading of the audio stream
        mLoopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateButtons(MediaManager.isPlayingAudio());
            }
        },750);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPoster.setKeepScreenOn(false);
        stopRotate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotate();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (MediaManager.isPlayingAudio()) MediaManager.pauseAudio(); else MediaManager.resumeAudio();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                MediaManager.nextAudioItem();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                MediaManager.prevAudioItem();
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private GotFocusEvent mainAreaFocusListener = new GotFocusEvent() {
        @Override
        public void gotFocus(View v) {
            //scroll so entire main area is in view
            mScrollView.smoothScrollTo(0, 0);
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
        }
    }

    private void updateButtons(boolean playing) {
        mPoster.setKeepScreenOn(playing);
        mPlayPauseButton.setState(!playing ? ImageButton.STATE_PRIMARY : ImageButton.STATE_SECONDARY);
        mRepeatButton.setState(MediaManager.isRepeatMode() ? ImageButton.STATE_SECONDARY : ImageButton.STATE_PRIMARY);
        mPrevButton.setEnabled(MediaManager.hasPrevAudioItem());
        mNextButton.setEnabled(MediaManager.hasNextAudioItem());
        mShuffleButton.setEnabled(MediaManager.getCurrentAudioQueueSize() > 1);
        mAlbumButton.setEnabled(mBaseItem.getAlbumId() != null);
        mArtistButton.setEnabled(mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0);
    }

    private void updateInfo(BaseItemDto item) {
        mArtistName.setText(item.getAlbumArtist());
        mSongTitle.setText(item.getName());
        mAlbumTitle.setText(item.getAlbum());
        mCurrentNdx.setText(MediaManager.getCurrentAudioQueueDisplayPosition());
        mTotal.setText(MediaManager.getCurrentAudioQueueDisplaySize());
        mCurrentDuration = ((Long)(item.getRunTimeTicks() / 10000)).intValue();
        //set progress to match duration
        mCurrentProgress.setMax(mCurrentDuration);
        addGenres(mGenreRow);
        updateBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true));

    }

    public void setCurrentTime(long time) {
            mCurrentProgress.setProgress(((Long) time).intValue());
            mCurrentPos.setText(Utils.formatMillis(time));
            mRemainingTime.setText(mCurrentDuration > 0 ? "-" + Utils.formatMillis(mCurrentDuration - time) : "");
    }

    private void addGenres(LinearLayout layout) {
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
            //todo - advance to this item in playlist
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            //todo - anything to do here...?
        }
    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
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

    protected void updateBackground(String url) {
        if (url == null) {
            BackgroundManager.getInstance(this).setDrawable(mDefaultBackground);
        } else {
            Picasso.with(this)
                    .load(url)
                    .skipMemoryCache()
                    .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
    }

}
