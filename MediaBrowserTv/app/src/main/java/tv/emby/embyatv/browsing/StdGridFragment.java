/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tv.emby.embyatv.browsing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.opengles.GL10;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.DisplayPreferences;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.model.FilterOptions;
import tv.emby.embyatv.model.PosterSize;
import tv.emby.embyatv.model.ViewType;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.HorizontalGridPresenter;
import tv.emby.embyatv.model.ImageType;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.search.SearchActivity;
import tv.emby.embyatv.ui.CharSelectedListener;
import tv.emby.embyatv.ui.DisplayPrefsPopup;
import tv.emby.embyatv.ui.HorizontalGridFragment;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.ui.JumpList;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.RemoteControlReceiver;
import tv.emby.embyatv.util.Utils;

public class StdGridFragment extends HorizontalGridFragment implements IGridLoader {
    private static final String TAG = "StdGridFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 100;

    protected String MainTitle;
    protected TvApp mApplication;
    protected BaseActivity mActivity;
    protected BaseRowItem mCurrentItem;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ItemRowAdapter mGridAdapter;
    private Drawable mDefaultBackground;
    private SimpleTarget<Bitmap> mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected BrowseRowDef mRowDef;
    CardPresenter mCardPresenter;

    protected boolean justLoaded = true;
    protected boolean ShowFanart = false;
    protected String mPosterSizeSetting = PosterSize.AUTO;
    protected String mImageType = ImageType.DEFAULT;
    protected boolean determiningPosterSize = false;

    protected String mParentId;
    protected BaseItemDto mFolder;
    protected DisplayPreferences mDisplayPrefs;

    private int mCardHeight = SMALL_CARD;

    protected boolean mAllowViewSelection = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"), BaseItemDto.class);
        mParentId = mFolder.getId();
        MainTitle = mFolder.getName();
        mDisplayPrefs = TvApp.getApplication().getCachedDisplayPrefs(mFolder.getDisplayPreferencesId()); //These should have already been loaded
        mPosterSizeSetting = mDisplayPrefs.getCustomPrefs().get("PosterSize");
        mImageType = mDisplayPrefs.getCustomPrefs().get("ImageType");
        if (mImageType == null) mImageType = ImageType.DEFAULT;
        if (mPosterSizeSetting == null) mPosterSizeSetting = PosterSize.AUTO;

        mCardHeight = getCardHeight(mPosterSizeSetting);

        setupUIElements();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApplication = TvApp.getApplication();
        if (getActivity() instanceof BaseActivity) mActivity = (BaseActivity)getActivity();

        prepareBackgroundManager();

        setupQueries(this);

        addTools();

        setupEventListeners();
    }

    protected void setupQueries(IGridLoader gridLoader) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

        ShowFanart = mApplication.getPrefs().getBoolean("pref_show_backdrop", true);

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mGridAdapter != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity.isFinishing()) return;
                        if (mGridAdapter != null && mGridAdapter.size() > 0) {
                            if (!mGridAdapter.ReRetrieveIfNeeded()) refreshCurrentItem();
                        }
                    }
                },500);
            }

        } else {
            justLoaded = false;
        }
    }

    public void setCardHeight(int height) {
        mCardHeight = height;
    }

    public int getCardHeight() {
        return mCardHeight;
    }

    protected void buildAdapter(BrowseRowDef rowDef) {
        mCardPresenter = new CardPresenter(false, mImageType, mCardHeight);

        switch (mRowDef.getQueryType()) {
            case NextUp:
                mGridAdapter = new ItemRowAdapter(mRowDef.getNextUpQuery(), true, mCardPresenter, null);
                break;
            case Season:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSeasonQuery(), mCardPresenter, null);
                break;
            case Upcoming:
                mGridAdapter = new ItemRowAdapter(mRowDef.getUpcomingQuery(), mCardPresenter, null);
                break;
            case Views:
                mGridAdapter = new ItemRowAdapter(new ViewQuery(), mCardPresenter, null);
                break;
            case SimilarSeries:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, null);
                break;
            case SimilarMovies:
                mGridAdapter = new ItemRowAdapter(mRowDef.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, null);
                break;
            case Persons:
                mGridAdapter = new ItemRowAdapter(mRowDef.getPersonsQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            case LiveTvChannel:
                mGridAdapter = new ItemRowAdapter(mRowDef.getTvChannelQuery(), 40, mCardPresenter, null);
                break;
            case LiveTvProgram:
                mGridAdapter = new ItemRowAdapter(mRowDef.getProgramQuery(), mCardPresenter, null);
                break;
            case LiveTvRecording:
                mGridAdapter = new ItemRowAdapter(mRowDef.getRecordingQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            case LiveTvRecordingGroup:
                mGridAdapter = new ItemRowAdapter(mRowDef.getRecordingGroupQuery(), mCardPresenter, null);
                break;
            case AlbumArtists:
                mGridAdapter = new ItemRowAdapter(mRowDef.getArtistsQuery(), mRowDef.getChunkSize(), mCardPresenter, null);
                break;
            default:
                mGridAdapter = new ItemRowAdapter(mRowDef.getQuery(), mRowDef.getChunkSize(), mRowDef.getPreferParentThumb(), mRowDef.isStaticHeight(), mCardPresenter, null);
                break;
        }

        FilterOptions filters = new FilterOptions();
        filters.setFavoriteOnly(Boolean.parseBoolean(mDisplayPrefs.getCustomPrefs().get("FavoriteOnly")));
        filters.setUnwatchedOnly(Boolean.parseBoolean(mDisplayPrefs.getCustomPrefs().get("UnwatchedOnly")));

        setupRetrieveListeners();
        mGridAdapter.setFilters(filters);
        setAdapter(mGridAdapter);


    }

    public void loadGrid(final BrowseRowDef rowDef) {
        determiningPosterSize = true;
        buildAdapter(rowDef);

        if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
            mGridAdapter.GetResultSizeAsync(new Response<Integer>() {
                @Override
                public void onResponse(Integer response) {
                    int autoHeight = getAutoCardHeight(response);
                    if (autoHeight != mCardHeight) {
                        mCardHeight = autoHeight;
                        setNumberOfRows();
                        createGrid();
                        TvApp.getApplication().getLogger().Debug("Auto card height is "+mCardHeight);
                        buildAdapter(rowDef);
                    }
                    mGridAdapter.setSortBy(getSortOption(mDisplayPrefs.getSortBy()));
                    mGridAdapter.Retrieve();
                    determiningPosterSize = false;
                }
            });
        } else {
            mGridAdapter.setSortBy(getSortOption(mDisplayPrefs.getSortBy()));
            mGridAdapter.Retrieve();
            determiningPosterSize = false;
        }

    }

    protected int getCardHeight(String heightSetting) {
        switch (heightSetting) {
            case PosterSize.MED:
                return mImageType.equals(ImageType.BANNER) ? MED_BANNER : MED_CARD;
            case PosterSize.LARGE:
                return mImageType.equals(ImageType.BANNER) ? LARGE_BANNER : LARGE_CARD;
            default:
                return mImageType.equals(ImageType.BANNER) ? SMALL_BANNER : SMALL_CARD;

        }
    }

    protected int getAutoCardHeight(Integer size) {
        TvApp.getApplication().getLogger().Debug("Result size for auto card height is " + size);
        if (size > 35)
            return getCardHeight(PosterSize.SMALL);
        else if (size > 10)
            return getCardHeight(PosterSize.MED);
        else
            return getCardHeight(PosterSize.LARGE);

    }
    private void prepareBackgroundManager() {

        final BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mBackgroundTarget = new SimpleTarget<Bitmap>(mMetrics.widthPixels, mMetrics.heightPixels) {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                backgroundManager.setBitmap(resource);
            }
        };
    }

    protected void setupUIElements() {

        HorizontalGridPresenter gridPresenter = new HorizontalGridPresenter();
        setGridPresenter(gridPresenter);
        setNumberOfRows();

        mJumplistPopup = new JumplistPopup(getActivity());
    }

    protected void setNumberOfRows() {
        // calculate number of rows based on card height
        getGridPresenter().setNumberOfRows(getGridHeight() / getCardHeight());

    }

    protected ImageButton mUnwatchedButton;
    protected ImageButton mFavoriteButton;
    protected ImageButton mLetterButton;
    protected DisplayPrefsPopup mDisplayPrefsPopup;

    protected void updateDisplayPrefs() {
        if (mDisplayPrefs.getCustomPrefs() == null)
            mDisplayPrefs.setCustomPrefs(new HashMap<String, String>());
        mDisplayPrefs.getCustomPrefs().put("UnwatchedOnly", mGridAdapter.getFilters().isUnwatchedOnly() ? "true" : "false");
        mDisplayPrefs.getCustomPrefs().put("FavoriteOnly", mGridAdapter.getFilters().isFavoriteOnly() ? "true" : "false");
        mDisplayPrefs.setSortBy(mGridAdapter.getSortBy());
        mDisplayPrefs.setSortOrder(getSortOption(mGridAdapter.getSortBy()).order);
        TvApp.getApplication().updateDisplayPrefs(mDisplayPrefs);
    }

    protected void addTools() {
        //Add tools
        LinearLayout toolBar = getToolBar();
        int size = Utils.convertDpToPixel(getActivity(), 24);

        mDisplayPrefsPopup = new DisplayPrefsPopup(getActivity(), mGridDock, mAllowViewSelection, new Response<Boolean>() {
            @Override
            public void onResponse(Boolean response) {
                TvApp.getApplication().updateDisplayPrefs(mDisplayPrefs);
                if (response)
                {
                    mImageType = mDisplayPrefs.getCustomPrefs().get("ImageType");
                    mPosterSizeSetting = mDisplayPrefs.getCustomPrefs().get("PosterSize");
                    mCardHeight = getCardHeight(mPosterSizeSetting);
                    setNumberOfRows();
                    createGrid();
                    loadGrid(mRowDef);
                }
            }
        });

        toolBar.addView(new ImageButton(getActivity(), R.drawable.sort, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create sort menu
                PopupMenu sortMenu = Utils.createPopupMenu(getActivity(), getToolBar(), Gravity.RIGHT);
                for (Integer key : sortOptions.keySet()) {
                    SortOption option = sortOptions.get(key);
                    if (option == null) option = sortOptions.get(0);
                    MenuItem item = sortMenu.getMenu().add(0, key, key, option.name);
                    if (option.value.equals(mDisplayPrefs.getSortBy())) item.setChecked(true);
                }
                sortMenu.getMenu().setGroupCheckable(0, true, true);
                sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mGridAdapter.setSortBy(sortOptions.get(item.getItemId()));
                        mGridAdapter.Retrieve();
                        item.setChecked(true);
                        updateDisplayPrefs();
                        return true;
                    }
                });
                sortMenu.show();
            }
        }));

        if (mRowDef.getQueryType() == QueryType.Items) {
            mUnwatchedButton = new ImageButton(getActivity(), mGridAdapter.getFilters().isUnwatchedOnly() ? R.drawable.unwatchedred : R.drawable.unwatchedwhite, size, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterOptions filters = mGridAdapter.getFilters();
                    if (filters == null) filters = new FilterOptions();

                    filters.setUnwatchedOnly(!filters.isUnwatchedOnly());
                    updateDisplayPrefs();
                    mGridAdapter.setFilters(filters);
                    if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
                        loadGrid(mRowDef);
                    } else {
                        mGridAdapter.Retrieve();
                    }
                    mUnwatchedButton.setImageResource(filters.isUnwatchedOnly() ? R.drawable.unwatchedred : R.drawable.unwatchedwhite);


                }
            });
            toolBar.addView(mUnwatchedButton);
        }

        mFavoriteButton =new ImageButton(getActivity(), mGridAdapter.getFilters().isFavoriteOnly() ? R.drawable.redheart : R.drawable.whiteheart, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mGridAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setFavoriteOnly(!filters.isFavoriteOnly());
                mGridAdapter.setFilters(filters);
                updateDisplayPrefs();
                if (mPosterSizeSetting.equals(PosterSize.AUTO)) {
                    loadGrid(mRowDef);
                } else {
                    mGridAdapter.Retrieve();
                }
                mFavoriteButton.setImageResource(filters.isFavoriteOnly() ? R.drawable.redheart : R.drawable.whiteheart);

            }
        });
        toolBar.addView(mFavoriteButton);

        mLetterButton = new ImageButton(getActivity(), R.drawable.jumpletter, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Open letter jump popup
                mJumplistPopup.show();
            }
        });
        toolBar.addView(mLetterButton);

        toolBar.addView(new ImageButton(getActivity(), R.drawable.search2, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvApp.getApplication().showSearch(getActivity(), "music".equals(mFolder.getCollectionType()) || "MusicAlbum".equals(mFolder.getType()) || "MusicArtist".equals(mFolder.getType()));
            }
        }));

        toolBar.addView(new ImageButton(getActivity(), R.drawable.cog, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDisplayPrefsPopup.show(mDisplayPrefs, mFolder.getCollectionType());
            }
        }));


    }

    private JumplistPopup mJumplistPopup;
    class JumplistPopup {

        final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 900);
        final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 55);

        PopupWindow mPopup;
        Activity mActivity;
        JumpList mJumplist;

        JumplistPopup(Activity activity) {
            mActivity = activity;
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.empty_popup, null);
            mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
            mPopup.setFocusable(true);
            mPopup.setOutsideTouchable(true);
            mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss
            mPopup.setAnimationStyle(R.style.PopupSlideInTop);

            mJumplist = new JumpList(activity, new CharSelectedListener() {
                @Override
                public void onCharSelected(String ch) {
                    mGridAdapter.setStartLetter(ch);
                    loadGrid(mRowDef);
                    dismiss();
                }
            });

            mJumplist.setGravity(Gravity.CENTER_HORIZONTAL);
            FrameLayout root = (FrameLayout) layout.findViewById(R.id.root);
            root.addView(mJumplist);

        }

        public boolean isShowing() {
            return (mPopup != null && mPopup.isShowing());
        }

        public void show() {

            mPopup.showAtLocation(mGridDock, Gravity.TOP, mGridDock.getLeft(), mGridDock.getTop());
            mJumplist.setFocus(mGridAdapter.getStartLetter());

        }

        public void dismiss() {
            if (mPopup != null && mPopup.isShowing()) {
                mPopup.dismiss();
            }
        }
    }

    protected void setupEventListeners() {

        setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        if (mActivity != null) {
            mActivity.registerKeyListener(new IKeyListener() {
                @Override
                public boolean onKeyUp(int key, KeyEvent event) {
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        MediaManager.setCurrentMediaAdapter(mGridAdapter);
                        MediaManager.setCurrentMediaPosition(mCurrentItem.getIndex());
                        MediaManager.setCurrentMediaTitle(mFolder.getName());
                    }
                    return KeyProcessor.HandleKey(key, mCurrentItem, mActivity);
                }
            });

            mActivity.registerMessageListener(new IMessageListener() {
                @Override
                public void onMessageReceived(CustomMessage message) {
                    switch (message) {

                        case RefreshCurrentItem:
                            refreshCurrentItem();
                            break;
                    }
                }
            });
        }
    }

    protected void setupRetrieveListeners() {
        mGridAdapter.setRetrieveStartedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                showSpinner();

            }
        });
        mGridAdapter.setRetrieveFinishedListener(new EmptyResponse() {
            @Override
            public void onResponse() {
                hideSpinner();
                setStatusText(mFolder.getName());
                updateCounter(mGridAdapter.getTotalItems() > 0 ? 1 : 0);
                mLetterButton.setVisibility("SortName".equals(mGridAdapter.getSortBy()) ? View.VISIBLE : View.GONE);
                setItem(null);
                if (mGridAdapter.getTotalItems() == 0) {
                    mToolBar.requestFocus();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setTitle(mFolder.getName());

                        }
                    }, 500);
                } else focusGrid();
            }
        });
    }

    private void refreshCurrentItem() {
        if (MediaManager.getCurrentMediaPosition() >= 0) {
            mCurrentItem = MediaManager.getCurrentMediaItem();
            getGridPresenter().setPosition(MediaManager.getCurrentMediaPosition());
            MediaManager.setCurrentMediaPosition(-1); // re-set so it doesn't mess with parent views
        }
        if (mCurrentItem != null && !"Photo".equals(mCurrentItem.getType()) && !"PhotoAlbum".equals(mCurrentItem.getType())
                && !"MusicArtist".equals(mCurrentItem.getType()) && !"MusicAlbum".equals(mCurrentItem.getType())) {
            TvApp.getApplication().getLogger().Debug("Refresh item "+mCurrentItem.getFullName());
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {

                    mGridAdapter.notifyArrayItemRangeChanged(mGridAdapter.indexOf(mCurrentItem), 1);
                    //Now - if filtered make sure we still pass
                    if (mGridAdapter.getFilters() != null) {
                        if ((mGridAdapter.getFilters().isFavoriteOnly() && !mCurrentItem.isFavorite()) || (mGridAdapter.getFilters().isUnwatchedOnly() && mCurrentItem.isPlayed())) {
                            //if we are about to remove last item, throw focus to toolbar so framework doesn't crash
                            if (mGridAdapter.size() == 1) mToolBar.requestFocus();
                            mGridAdapter.remove(mCurrentItem);
                            mGridAdapter.setTotalItems(mGridAdapter.getTotalItems() - 1);
                            updateCounter(mCurrentItem.getIndex());
                        }
                    }
                }
            });

        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, mGridAdapter, ((BaseRowItem)item).getIndex(), getActivity());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            if (ShowFanart) {
                mBackgroundUrl = mCurrentItem.getBackdropImageUrl();
                startBackgroundTimer();
            }
            setItem(mCurrentItem);
        }
    };

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            mHandler.removeCallbacks(mDelayedSetItem);
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                setTitle(MainTitle);
                //fill in default background
                mBackgroundUrl = null;
                startBackgroundTimer();
            } else {
                mCurrentItem = (BaseRowItem)item;
                mTitleView.setText(mCurrentItem.getName());
                mInfoRow.removeAllViews();
                mHandler.postDelayed(mDelayedSetItem, 400);

                if (!determiningPosterSize) mGridAdapter.loadMoreItemsIfNeeded(mCurrentItem.getIndex());

            }

        }
    }

    protected void setDefaultBackground(Drawable background) {
        mDefaultBackground = background;
    }

    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = getResources().getDrawable(resourceId);
    }

    protected void updateBackground(String url) {
        Glide.with(getActivity())
                .load(url)
                .asBitmap()
                .override(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    protected void updateBackground(Drawable drawable) {
        BackgroundManager.getInstance(getActivity()).setDrawable(drawable);
    }

    protected void clearBackground() {
        BackgroundManager.getInstance(getActivity()).setDrawable(mDefaultBackground);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundUrl != null) {
                        updateBackground(mBackgroundUrl);
                    } else {
                        updateBackground(mDefaultBackground);
                    }
                }
            });

        }
    }


}
