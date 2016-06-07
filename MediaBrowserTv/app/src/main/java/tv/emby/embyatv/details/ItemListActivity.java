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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.playlists.PlaylistItemQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.model.GotFocusEvent;
import tv.emby.embyatv.playback.AudioEventListener;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackController;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.querying.StdItemQuery;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.ui.ItemListView;
import tv.emby.embyatv.ui.ItemRowView;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 11/22/2015.
 */
public class ItemListActivity extends BaseActivity {

    private int BUTTON_SIZE;
    public static final String FAV_SONGS = "FAV_SONGS";
    public static final String VIDEO_QUEUE = "VIDEO_QUEUE";

    private TextView mTitle;
    private LinearLayout mGenreRow;
    private ImageView mPoster;
    private TextView mButtonHelp;
    private TextView mSummaryTitle;
    private TextView mTimeLine;
    private TextView mSummary;
    private LinearLayout mButtonRow;
    private ImageView mStudioImage;
    private ItemListView mItemList;
    private ScrollView mScrollView;
    private ItemRowView mCurrentRow;

    private ItemRowView mCurrentlyPlayingRow;

    private BaseItemDto mBaseItem;
    private List<BaseItemDto> mItems = new ArrayList<>();
    private String mItemId;

    private int mBottomScrollThreshold;
    private Runnable mClockLoop;

    private TvApp mApplication;
    private BaseActivity mActivity;
    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;

    private boolean firstTime = true;
    private Calendar lastUpdated = Calendar.getInstance();

    private Typeface roboto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

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
        mItemList = (ItemListView) findViewById(R.id.songs);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        mBottomScrollThreshold = (int)(mMetrics.heightPixels *.6);

        //Item list listeners
        mItemList.setRowSelectedListener(new ItemRowView.RowSelectedListener() {
            @Override
            public void onRowSelected(ItemRowView row) {
                mCurrentRow = row;
                //Keep selected row in center of screen
                int[] location = new int[] {0,0};
                row.getLocationOnScreen(location);
                int y = location[1];
                if (y > mBottomScrollThreshold) {
                    // too close to bottom - scroll down
                    mScrollView.smoothScrollBy(0, y - mBottomScrollThreshold);
                }
                //TvApp.getApplication().getLogger().Debug("Row selected: "+row.getItem().getName()+" at "+location[1]+" Screen edge: "+mMetrics.heightPixels);
            }
        });

        mItemList.setRowClickedListener(new ItemRowView.RowClickedListener() {
            @Override
            public void onRowClicked(ItemRowView row) {
                showMenu(row, !"Audio".equals(row.getItem().getType()));
            }
        });

        //Adjust layout for our display - no summary title
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTimeLine.getLayoutParams();
        params.topMargin = 20;
        mSummaryTitle.setVisibility(View.GONE);

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
                if (MediaManager.isPlayingAudio()) {
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
                        case KeyEvent.KEYCODE_MENU:
                            showMenu(mCurrentRow, false);
                            return true;
                        }
                } else if (mCurrentRow != null){
                    switch (key) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_MENU:
                            showMenu(mCurrentRow, false);
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
        startClock();
        MediaManager.addAudioEventListener(mAudioEventListener);
        // and fire it to be sure we're updated
        mAudioEventListener.onPlaybackStateChange(MediaManager.isPlayingAudio() ? PlaybackController.PlaybackState.PLAYING : PlaybackController.PlaybackState.IDLE, MediaManager.getCurrentAudioItem());

        if (!firstTime && mApplication.getLastPlayback().after(lastUpdated)) {
            if (mItemId.equals(VIDEO_QUEUE)) {
                //update this in case it changed - delay to allow for the changes
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItems = MediaManager.getCurrentVideoQueue();
                        if (mItems != null && mItems.size() > 0) {
                            mItemList.clear();
                            mCurrentRow = null;
                            mItemList.addItems(mItems);
                            lastUpdated = Calendar.getInstance();
                        } else {
                            //nothing left in queue
                            finish();
                        }
                    }
                }, 750);
            } else if ("Video".equals(mBaseItem.getMediaType())) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mItemList.refresh();
                        lastUpdated = Calendar.getInstance();

                    }
                }, 500);
            }
        }

        firstTime = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRotate();
        stopClock();
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
                mCurrentlyPlayingRow = mItemList.updatePlaying(null);
            } else {
                mCurrentlyPlayingRow = mItemList.updatePlaying(currentItem.getId());
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

    private void showMenu(final ItemRowView row, boolean showOpen) {
        PopupMenu menu = Utils.createPopupMenu(this, this.getCurrentFocus(), Gravity.RIGHT);
        int order = 0;
        if (showOpen) {
            MenuItem open = menu.getMenu().add(0, 0, order++, R.string.lbl_open);
            open.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    ItemLauncher.launch(new BaseRowItem(0, row.getItem()), null, 0, mActivity);
                    return true;
                }
            });

        }
        MenuItem playFromHere = menu.getMenu().add(0, 0, order++, R.string.lbl_play_from_here);
        playFromHere.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems.subList(row.getIndex(), mItems.size()));
                return true;
            }
        });
        MenuItem play = menu.getMenu().add(0, 1, order++, R.string.lbl_play);
        play.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                play(mItems.subList(row.getIndex(), row.getIndex()+1));
                return true;
            }
        });
        MenuItem queue = menu.getMenu().add(0, 2, order++, R.string.lbl_add_to_queue);
        queue.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (row.getItem().getMediaType()) {
                    case "Video":
                        MediaManager.addToVideoQueue(row.getItem());
                        break;
                    case "Audio":
                        MediaManager.queueAudioItem(row.getItem());
                        break;
                }
                return true;
            }
        });
        if ("Audio".equals(row.getItem().getType())) {
            MenuItem mix = menu.getMenu().add(0, 1, order++, R.string.lbl_instant_mix);
            mix.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Utils.playInstantMix(row.getItem().getId());
                    return true;
                }
            });

        }

        menu.show();

    }

    private void loadItem(String id) {
        //Special case handling
        switch (id) {
            case FAV_SONGS:
                BaseItemDto item = new BaseItemDto();
                item.setId(FAV_SONGS);
                item.setName(getString(R.string.lbl_favorites));
                item.setOverview(getString(R.string.desc_automatic_fav_songs));
                item.setPlayAccess(PlayAccess.Full);
                item.setMediaType("Audio");
                item.setType("Playlist");
                item.setIsFolder(true);
                setBaseItem(item);
                break;
            case VIDEO_QUEUE:
                BaseItemDto queue = new BaseItemDto();
                queue.setId(VIDEO_QUEUE);
                queue.setName(getString(R.string.lbl_current_queue));
                queue.setOverview(getString(R.string.desc_current_video_queue));
                queue.setPlayAccess(PlayAccess.Full);
                queue.setMediaType("Video");
                queue.setType("Playlist");
                queue.setIsFolder(true);
                if (MediaManager.getCurrentVideoQueue() != null) {
                    long runtime = 0;
                    int children = 0;
                    for (BaseItemDto video : MediaManager.getCurrentVideoQueue()) {
                        runtime += video.getRunTimeTicks() != null ? video.getRunTimeTicks() : 0;
                        children += 1;
                    }
                    queue.setCumulativeRunTimeTicks(runtime);
                    queue.setChildCount(children);
                }
                setBaseItem(queue);
                break;
            default:
                mApplication.getApiClient().GetItemAsync(id, mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        setBaseItem(response);
                    }
                });
                break;
        }
    }

    public void setBaseItem(BaseItemDto item) {
        mBaseItem = item;

        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(this, item, mainInfoRow, false, false);
        addGenres(mGenreRow);
        addButtons(BUTTON_SIZE);
        mSummary.setText(mBaseItem.getOverview());
        mTimeLine.setText(getEndTime());

        updatePoster(mBaseItem);

        //get items
        if ("Playlist".equals(mBaseItem.getType())) {
            // Have to use different query here
            switch (mItemId) {
                case FAV_SONGS:
                    //Get favorited and liked songs from this area
                    StdItemQuery favSongs = new StdItemQuery(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
                    favSongs.setParentId(getIntent().getStringExtra("ParentId"));
                    favSongs.setIncludeItemTypes(new String[] {"Audio"});
                    favSongs.setRecursive(true);
                    favSongs.setFilters(new ItemFilter[]{ItemFilter.IsFavoriteOrLikes});
                    favSongs.setSortBy(new String[]{ItemSortBy.Random});
                    favSongs.setLimit(150);
                    TvApp.getApplication().getApiClient().GetItemsAsync(favSongs, itemResponse);
                    break;
                case VIDEO_QUEUE:
                    //Show current queue
                    mTitle.setText(mBaseItem.getName());
                    mItemList.addItems(MediaManager.getCurrentVideoQueue());
                    mItems.addAll(MediaManager.getCurrentVideoQueue());
                    updateBackdrop();
                    break;
                default:
                    PlaylistItemQuery playlistSongs = new PlaylistItemQuery();
                    playlistSongs.setId(mBaseItem.getId());
                    playlistSongs.setUserId(TvApp.getApplication().getCurrentUser().getId());
                    playlistSongs.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Genres, ItemFields.Chapters});
                    playlistSongs.setLimit(150);
                    TvApp.getApplication().getApiClient().GetPlaylistItems(playlistSongs, itemResponse);
                    break;
            }
        } else {
            StdItemQuery songs = new StdItemQuery();
            songs.setParentId(mBaseItem.getId());
            songs.setRecursive(true);
            songs.setFields(new ItemFields[]{ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
            songs.setIncludeItemTypes(new String[]{"Audio"});
            songs.setSortBy(new String[] {ItemSortBy.SortName});
            songs.setLimit(200);
            mApplication.getApiClient().GetItemsAsync(songs, itemResponse);
        }


    }

    private Response<ItemsResult> itemResponse = new Response<ItemsResult>() {
        @Override
        public void onResponse(ItemsResult response) {
            mTitle.setText(mBaseItem.getName());
            if (mBaseItem.getName().length() > 32) {
                // scale down the title so more will fit
                mTitle.setTextSize(32);
            }
            if (response.getTotalRecordCount() > 0) {
                mItems = new ArrayList<>();
                int i = 0;
                for (BaseItemDto item : response.getItems()) {
                    mItemList.addItem(item, i++);
                    mItems.add(item);
                }
                if (MediaManager.isPlayingAudio()) {
                    //update our status
                    mAudioEventListener.onPlaybackStateChange(PlaybackController.PlaybackState.PLAYING, MediaManager.getCurrentAudioItem());
                }
                updateBackdrop();
            }
        }

        @Override
        public void onError(Exception exception) {
            mApplication.getLogger().ErrorException("Error loading", exception);
            Utils.showToast(mActivity, exception.getLocalizedMessage());
        }
    };

    private void updatePoster(BaseItemDto item){
        switch (mItemId) {
            case FAV_SONGS:
                mPoster.setImageResource(R.drawable.genericmusic);
                break;
            case VIDEO_QUEUE:
                mPoster.setImageResource(R.drawable.transplaylist);
                break;
            default:
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

                break;
        }
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

    private String getEndTime() {
        if (mBaseItem != null) {
            Long runtime = mBaseItem.getCumulativeRunTimeTicks();
            if (runtime != null && runtime > 0) {
                long endTimeTicks = System.currentTimeMillis() + runtime / 10000;
                return getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
            }

        }
        return "";
    }

    private void startClock() {
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                mTimeLine.setText(getEndTime());
                mLoopHandler.postDelayed(this, 15000);
            }
        };

        mLoopHandler.postDelayed(mClockLoop, 15000);
    }

    private void stopClock() {
        if (mLoopHandler != null && mClockLoop != null) {
            mLoopHandler.removeCallbacks(mClockLoop);
        }
    }

    private void play(List<BaseItemDto> items) {
        if ("Video".equals(mBaseItem.getMediaType())) {
            Intent intent = new Intent(mActivity, PlaybackOverlayActivity.class);
            //Resume first item if needed
            BaseItemDto first = items.size() > 0 ? items.get(0) : null;
            if (first != null && first.getUserData() != null) {
                Long pos = first.getUserData().getPlaybackPositionTicks() / 10000;
                intent.putExtra("Position", pos.intValue());
            }
            MediaManager.setCurrentVideoQueue(items);
            startActivity(intent);

        } else {
            MediaManager.playNow(items);

        }

    }

    private void addButtons(int buttonSize) {
        if (Utils.CanPlay(mBaseItem)) {
            ImageButton play = new ImageButton(this, R.drawable.play, buttonSize, getString(mBaseItem.getIsFolder() ? R.string.lbl_play_all : R.string.lbl_play), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItems.size() > 0) {
                        play(mItems);
                    } else {
                        Utils.showToast(mActivity, R.string.msg_no_playable_items);
                    }
                }
            });
            play.setGotFocusListener(mainAreaFocusListener);
            mButtonRow.addView(play);
            play.requestFocus();
            if (mBaseItem.getIsFolder()) {
                ImageButton shuffle = new ImageButton(this, R.drawable.shuffle, buttonSize, getString(R.string.lbl_shuffle_all), mButtonHelp, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItems.size() > 0) {
                            if (mBaseItem.getId().equals(VIDEO_QUEUE) || mBaseItem.getId().equals(FAV_SONGS)) {
                                List<BaseItemDto> shuffled = new ArrayList<>(mItems);
                                Collections.shuffle(shuffled);
                                play(shuffled);
                            } else {
                                //use server retrieval in order to get all items
                                Utils.retrieveAndPlay(mBaseItem.getId(), true, mActivity);
                            }

                        } else {
                            Utils.showToast(mActivity, R.string.msg_no_playable_items);
                        }
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

        if (!mItemId.equals(FAV_SONGS)) {
            if (!mItemId.equals(VIDEO_QUEUE)) {
                //Favorite
                ImageButton fav = new ImageButton(this, mBaseItem.getUserData().getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart, buttonSize, getString(R.string.lbl_toggle_favorite), mButtonHelp, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        UserItemDataDto data = mBaseItem.getUserData();
                        mApplication.getApiClient().UpdateFavoriteStatusAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
                                mBaseItem.setUserData(response);
                                ((ImageButton) v).setImageResource(response.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart);
                                TvApp.getApplication().setLastFavoriteUpdate(System.currentTimeMillis());
                            }
                        });
                    }
                });
                mButtonRow.addView(fav);

            }

            if ("Playlist".equals(mBaseItem.getType())) {
                if (VIDEO_QUEUE.equals(mBaseItem.getId())) {
                    mButtonRow.addView(new ImageButton(this, R.drawable.saveplaylist, buttonSize, getString(R.string.lbl_save_as_playlist), mButtonHelp, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MediaManager.saveVideoQueue(mActivity);
                        }
                    }));
                }

                ImageButton delete = new ImageButton(this, R.drawable.trash, buttonSize, getString(R.string.lbl_delete), mButtonHelp, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (mBaseItem.getId().equals(VIDEO_QUEUE)) {
                            new AlertDialog.Builder(mActivity)
                                    .setTitle(R.string.lbl_clear_queue)
                                    .setMessage("Clear current video queue?")
                                    .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            MediaManager.setCurrentVideoQueue(new ArrayList<BaseItemDto>());
                                            mApplication.setLastVideoQueueChange(System.currentTimeMillis());
                                            finish();
                                        }
                                    })
                                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();

                        } else {
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
                    }
                });

                mButtonRow.addView(delete);
            }
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

    private BaseItemDto getRandomListItem() {
        if (mItems == null || mItems.size() == 0) return null;

        return mItems.get(Utils.randInt(0, mItems.size() - 1));
    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackdrop();
                mLoopHandler.postDelayed(this, FullDetailsActivity.BACKDROP_ROTATION_INTERVAL);
            }
        };

        mLoopHandler.postDelayed(mBackdropLoop, FullDetailsActivity.BACKDROP_ROTATION_INTERVAL);
    }

    private void updateBackdrop() {
        String url = Utils.getBackdropImageUrl(mBaseItem, mApplication.getApiClient(), true);
        if (url == null) {
            BaseItemDto item = getRandomListItem();
            if (item != null) url = Utils.getBackdropImageUrl(item, mApplication.getApiClient(), true);
        }
        if (url != null) updateBackground(url);

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
                    .centerCrop()
                    .error(mDefaultBackground)
                    .into(mBackgroundTarget);
        }
    }

}
