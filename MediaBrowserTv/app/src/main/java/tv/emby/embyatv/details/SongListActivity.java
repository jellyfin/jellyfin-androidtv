package tv.emby.embyatv.details;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.playlists.PlaylistItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.model.GotFocusEvent;
import tv.emby.embyatv.playback.AudioEventListener;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackController;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.ui.NowPlayingBug;
import tv.emby.embyatv.ui.SongListView;
import tv.emby.embyatv.ui.SongRowView;
import tv.emby.embyatv.util.DelayedMessage;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 11/22/2015.
 */
public class SongListActivity extends BaseActivity {

    private int BUTTON_SIZE;

    private TextView mTitle;
    private LinearLayout mGenreRow;
    private ImageView mPoster;
    private TextView mButtonHelp;
    private TextView mSummaryTitle;
    private TextView mTimeLine;
    private TextView mSummary;
    private LinearLayout mButtonRow;
    private ImageView mStudioImage;
    private SongListView mSongList;
    private ScrollView mScrollView;

    private SongRowView mCurrentlyPlayingRow;

    private BaseItemDto mBaseItem;
    private List<BaseItemDto> mSongs;
    private String mItemId;

    private int mBottomScrollThreshold;
    private int mTopScrollThreshold;

    private TvApp mApplication;
    private BaseActivity mActivity;
    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;

    private Typeface roboto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        mApplication = TvApp.getApplication();
        mActivity = this;
        roboto = mApplication.getDefaultFont();
        BUTTON_SIZE = Utils.convertDpToPixel(this, 35);

        mTitle = (TextView) findViewById(R.id.fdTitle);
        mTitle.setTypeface(roboto);
        mTitle.setShadowLayer(5, 5, 5, Color.BLACK);
        mGenreRow = (LinearLayout) findViewById(R.id.fdGenreRow);
        mPoster = (ImageView) findViewById(R.id.fdPoster);
        mStudioImage = (ImageView) findViewById(R.id.studioImage);
        mButtonHelp = (TextView) findViewById(R.id.fdButtonHelp);
        mButtonRow = (LinearLayout) findViewById(R.id.fdButtonRow);
        mSummaryTitle = (TextView) findViewById(R.id.fdSummaryTitle);
        mTimeLine = (TextView) findViewById(R.id.fdSummarySubTitle);
        mSummary = (TextView) findViewById(R.id.fdSummaryText);
        mSummary.setTypeface(roboto);
        mSongList = (SongListView) findViewById(R.id.songs);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mBottomScrollThreshold = (int)(mMetrics.heightPixels *.6);
        mTopScrollThreshold = (int)(mMetrics.heightPixels *.3);

        //Song list listeners
        mSongList.setRowSelectedListener(new SongRowView.RowSelectedListener() {
            @Override
            public void onRowSelected(SongRowView row) {
                //Keep selected row in center of screen
                int[] location = new int[] {0,0};
                row.getLocationOnScreen(location);
                int y = location[1];
                if (y > mBottomScrollThreshold) {
                    // too close to bottom - scroll down
                    mScrollView.smoothScrollBy(0, y - mBottomScrollThreshold);
                }
                //TvApp.getApplication().getLogger().Debug("Row selected: "+row.getSong().getName()+" at "+location[1]+" Screen edge: "+mMetrics.heightPixels);
            }
        });

        mSongList.setRowClickedListener(new SongRowView.RowClickedListener() {
            @Override
            public void onRowClicked(SongRowView row) {
                KeyProcessor.HandleKey(KeyEvent.KEYCODE_MENU, new BaseRowItem(0, row.getSong()), mActivity);
            }
        });

        //Adjust layout for our display - no timeline or summary title
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSummary.getLayoutParams();
        params.topMargin = 20;
        mSummary.setHeight(Utils.convertDpToPixel(TvApp.getApplication(), 235));
        mSummary.setMaxLines(12);
        mSummaryTitle.setVisibility(View.GONE);
        mTimeLine.setVisibility(View.GONE);

        mButtonRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) mButtonHelp.setText("");
            }
        });

        //Key listener
        registerKeyListener(new IKeyListener() {
            @Override
            public boolean onKeyUp(int key, KeyEvent event) {
                if (MediaManager.hasAudioQueueItems()) {
                    switch (key) {
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
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
                }
                return false;
            }
        });

        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);
        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mItemId = getIntent().getStringExtra("ItemId");
        loadItem(mItemId);

    }

    @Override
    protected void onResume() {
        super.onResume();
        rotateBackdrops();
        MediaManager.addAudioEventListener(mAudioEventListener);
        // and fire it to be sure we're updated
        mAudioEventListener.onPlaybackStateChange(MediaManager.isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.IDLE, MediaManager.getCurrentAudioItem());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRotate();
        MediaManager.removeAudioEventListener(mAudioEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotate();
    }

    private AudioEventListener mAudioEventListener = new AudioEventListener() {
        @Override
        public void onPlaybackStateChange(PlaybackController.PlaybackState newState, BaseItemDto currentItem) {
            TvApp.getApplication().getLogger().Info("Got playback state change event "+newState+" for item "+(currentItem != null ? currentItem.getName() : "<unknown>"));

            if (newState != PlaybackController.PlaybackState.PLAYING || currentItem == null) {
                if (mCurrentlyPlayingRow != null) mCurrentlyPlayingRow.updateCurrentTime(-1);
                mCurrentlyPlayingRow = mSongList.updatePlaying(null);
            } else {
                mCurrentlyPlayingRow = mSongList.updatePlaying(currentItem.getId());
            }
        }

        @Override
        public void onProgress(long pos) {
            if (mCurrentlyPlayingRow != null) {
                mCurrentlyPlayingRow.updateCurrentTime(pos);
            }
        }
    };

    private GotFocusEvent mainAreaFocusListener = new GotFocusEvent() {
        @Override
        public void gotFocus(View v) {
            //scroll so entire main area is in view
            mScrollView.smoothScrollTo(0, 0);
        }
    };

    private void loadItem(String id) {
        mApplication.getApiClient().GetItemAsync(id, mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                setBaseItem(response);
            }
        });
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;

        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(this, item, mainInfoRow, false, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());

        updateBackground(Utils.getBackdropImageUrl(item, TvApp.getApplication().getApiClient(), true));
        updatePoster(mBaseItem);

        //get songs
        if ("Playlist".equals(mBaseItem.getType())) {
            // Have to use different query here
            PlaylistItemQuery playlistSongs = new PlaylistItemQuery();
            playlistSongs.setId(mBaseItem.getId());
            playlistSongs.setUserId(TvApp.getApplication().getCurrentUser().getId());
            playlistSongs.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
            playlistSongs.setLimit(200);
            TvApp.getApplication().getApiClient().GetPlaylistItems(playlistSongs, songResponse);
        } else {
            StdItemQuery songs = new StdItemQuery();
            songs.setParentId(mBaseItem.getId());
            songs.setRecursive(true);
            songs.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
            songs.setIncludeItemTypes(new String[]{"Audio"});
            songs.setSortBy(new String[] {ItemSortBy.SortName});
            songs.setLimit(200);
            mApplication.getApiClient().GetItemsAsync(songs, songResponse);
        }


    }

    private Response<ItemsResult> songResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            mTitle.setText(mBaseItem.getName());
            if (mBaseItem.getName().length() > 32) {
                // scale down the title so more will fit
                mTitle.setTextSize(32);
            }
            if (response.getTotalRecordCount() > 0) {
                mSongs = new ArrayList<>();
                int i = 0;
                for (BaseItemDto item : response.getItems()) {
                    if ("Audio".equals(item.getType())) {
                        mSongList.addSong(item, i++);
                        mSongs.add(item);
                    }
                }
                if (MediaManager.isPlayingAudio()) {
                    //update our status
                    mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, MediaManager.getCurrentAudioItem());
                }
            }
        }

        @Override
        public void onError(Exception exception) {
            Utils.showToast(mActivity, exception.getLocalizedMessage());
        }
    };

    private void updatePoster(BaseItemDto item){
        // Figure image size
        Double aspect = Utils.getImageAspectRatio(item, false);
        int posterHeight = aspect > 1 ? Utils.convertDpToPixel(this, 170) : Utils.convertDpToPixel(this, 300);
        int posterWidth = (int)((aspect) * posterHeight);
        if (posterHeight < 10) posterWidth = Utils.convertDpToPixel(this, 150);  //Guard against zero size images causing picasso to barf

        String primaryImageUrl = Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(),false, false, posterHeight);

        Picasso.with(this)
                .load(primaryImageUrl)
                .resize(posterWidth,posterHeight)
                .centerInside()
                .into(mPoster);

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

    private void addButtons(int buttonSize) {
        if (Utils.CanPlay(mBaseItem)) {
            ImageButton play = new ImageButton(this, R.drawable.play, buttonSize, getString(Utils.isLiveTv(mBaseItem) ? R.string.lbl_tune_to_channel : mBaseItem.getIsFolder() ? R.string.lbl_play_all : R.string.lbl_play), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MediaManager.playNow(mSongs);
                }
            });
            play.setGotFocusListener(mainAreaFocusListener);
            mButtonRow.addView(play);
            play.requestFocus();
            if (mBaseItem.getIsFolder()) {
                ImageButton shuffle = new ImageButton(this, R.drawable.shuffle, buttonSize, getString(R.string.lbl_shuffle_all), mButtonHelp, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<BaseItemDto> shuffled = new ArrayList<>(mSongs);
                        Collections.shuffle(shuffled);
                        MediaManager.playNow(shuffled);
                    }
                });
                mButtonRow.addView(shuffle);
            }
        }

        if ("MusicAlbum".equals(mBaseItem.getType())) {
            ImageButton mix = new ImageButton(this, R.drawable.mix, buttonSize, getString(R.string.lbl_instant_mix), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Utils.Beep();
                    Utils.playInstantMix(mBaseItem.getId());
                }
            });
            mButtonRow.addView(mix);
        }

        //Favorite
        ImageButton fav = new ImageButton(this, mBaseItem.getUserData().getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart, buttonSize, getString(R.string.lbl_toggle_favorite), mButtonHelp, new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                UserItemDataDto data = mBaseItem.getUserData();
                mApplication.getApiClient().UpdateFavoriteStatusAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                    @Override
                    public void onResponse(UserItemDataDto response) {
                        mBaseItem.setUserData(response);
                        ((ImageButton)v).setImageResource(response.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart);
                    }
                });
            }
        });
        mButtonRow.addView(fav);

        if ("Playlist".equals(mBaseItem.getType())) {
            ImageButton delete = new ImageButton(this, R.drawable.trash, buttonSize, getString(R.string.lbl_delete), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.lbl_delete)
                            .setMessage("This will PERMANENTLY DELETE " + mBaseItem.getName() + " from your library.  Are you VERY sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    TvApp.getApplication().getApiClient().DeleteItem(mBaseItem.getId(), new EmptyResponse() {
                                        @Override
                                        public void onResponse() {
                                            Utils.showToast(mActivity, mBaseItem.getName() + " Deleted");
                                            TvApp.getApplication().setLastDeletedItemId(mBaseItem.getId());
                                            finish();
                                        }

                                        @Override
                                        public void onError(Exception ex) {
                                            Utils.showToast(mActivity, ex.getLocalizedMessage());
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Utils.showToast(mActivity, "Item NOT Deleted");
                                }
                            })
                            .show();

                }
            });

            mButtonRow.addView(delete);

        }

        if (mBaseItem.getAlbumArtists() != null && mBaseItem.getAlbumArtists().size() > 0) {
            ImageButton artist = new ImageButton(this, R.drawable.user, buttonSize, getString(R.string.lbl_open_artist), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent artist = new Intent(mActivity, FullDetailsActivity.class);
                    artist.putExtra("ItemId", mBaseItem.getAlbumArtists().get(0).getId());
                    mActivity.startActivity(artist);

                }
            });
            mButtonRow.addView(artist);
        }

    }
    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
                mLoopHandler.postDelayed(this, FullDetailsActivity.BACKDROP_ROTATION_INTERVAL);
            }
        };

        mLoopHandler.postDelayed(mBackdropLoop, FullDetailsActivity.BACKDROP_ROTATION_INTERVAL);
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
