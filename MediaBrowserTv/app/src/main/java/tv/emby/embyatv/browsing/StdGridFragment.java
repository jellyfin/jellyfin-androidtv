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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.DisplayPreferences;
import mediabrowser.model.entities.SortOrder;
import mediabrowser.model.querying.ItemSortBy;
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
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.HorizontalGridPresenter;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.search.SearchActivity;
import tv.emby.embyatv.ui.ClockUserView;
import tv.emby.embyatv.ui.HorizontalGridFragment;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.ui.ItemPanel;
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
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected BrowseRowDef mRowDef;
    CardPresenter mCardPresenter;

    protected boolean justLoaded = true;
    protected String mPosterSizeSetting = "auto";
    protected boolean determiningPosterSize = false;

    protected String mParentId;
    protected BaseItemDto mFolder;
    protected DisplayPreferences mDisplayPrefs;

    private int mCardHeight = SMALL_CARD;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFolder = TvApp.getApplication().getSerializer().DeserializeFromString(getActivity().getIntent().getStringExtra("Folder"), BaseItemDto.class);
        mParentId = mFolder.getId();
        MainTitle = mFolder.getName();
        mDisplayPrefs = TvApp.getApplication().getCachedDisplayPrefs(mFolder.getDisplayPreferencesId()); //These should have already been loaded
        mPosterSizeSetting = mDisplayPrefs.getCustomPrefs().get("PosterSize");
        if (mPosterSizeSetting == null) mPosterSizeSetting = "auto";

        switch (mPosterSizeSetting) {
            case "large":
                mCardHeight = LARGE_CARD;
                break;
            case "med":
                mCardHeight = MED_CARD;
                break;
            default:
                mCardHeight = SMALL_CARD;
        }

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
        //UnRegister the media button receiver
        AudioManager audioManager = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        audioManager.unregisterMediaButtonEventReceiver(new ComponentName(getActivity().getPackageName(), RemoteControlReceiver.class.getName()));

        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

        //Register a media button receiver so that all media button presses will come to us and not another app
        AudioManager audioManager = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerMediaButtonEventReceiver(new ComponentName(getActivity().getPackageName(), RemoteControlReceiver.class.getName()));
        //TODO implement conditional logic for api 21+

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
        mCardPresenter = new CardPresenter(false, mCardHeight);

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
                mGridAdapter = new ItemRowAdapter(mRowDef.getRecordingQuery(), mCardPresenter, null);
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

        if (mPosterSizeSetting.equals("auto")) {
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

    protected int getAutoCardHeight(Integer size) {
        TvApp.getApplication().getLogger().Debug("Result size for auto card height is "+size);
        if (size > 35)
            return SMALL_CARD;
        else if (size > 10)
            return MED_CARD;
        else
            return LARGE_CARD;

    }
    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void setupUIElements() {

        HorizontalGridPresenter gridPresenter = new HorizontalGridPresenter();
        setGridPresenter(gridPresenter);
        setNumberOfRows();
    }

    protected void setNumberOfRows() {
        // calculate number of rows based on card height
        getGridPresenter().setNumberOfRows(getGridHeight() / getCardHeight());

    }

    protected ImageButton mUnwatchedButton;
    protected ImageButton mFavoriteButton;

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
                        item.setChecked(true);
                        updateDisplayPrefs();
                        return true;
                    }
                });
                sortMenu.show();
            }
        }));

        mUnwatchedButton =new ImageButton(getActivity(), mGridAdapter.getFilters().isUnwatchedOnly() ? R.drawable.unwatchedred : R.drawable.unwatchedwhite, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mGridAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setUnwatchedOnly(!filters.isUnwatchedOnly());
                updateDisplayPrefs();
                mGridAdapter.setFilters(filters);
                if (mPosterSizeSetting.equals("auto")) {
                    loadGrid(mRowDef);
                } else {
                    mGridAdapter.Retrieve();
                }
                mUnwatchedButton.setImageResource(filters.isUnwatchedOnly() ? R.drawable.unwatchedred : R.drawable.unwatchedwhite);


            }
        });
        toolBar.addView(mUnwatchedButton);

        mFavoriteButton =new ImageButton(getActivity(), mGridAdapter.getFilters().isFavoriteOnly() ? R.drawable.redheart : R.drawable.whiteheart, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterOptions filters = mGridAdapter.getFilters();
                if (filters == null) filters = new FilterOptions();

                filters.setFavoriteOnly(!filters.isFavoriteOnly());
                mGridAdapter.setFilters(filters);
                updateDisplayPrefs();
                if (mPosterSizeSetting.equals("auto")) {
                    loadGrid(mRowDef);
                } else {
                    mGridAdapter.Retrieve();
                }
                mFavoriteButton.setImageResource(filters.isFavoriteOnly() ? R.drawable.redheart : R.drawable.whiteheart);

            }
        });
        toolBar.addView(mFavoriteButton);

        toolBar.addView(new ImageButton(getActivity(), R.drawable.search2, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                getActivity().startActivity(intent);
            }
        }));

        toolBar.addView(new ImageButton(getActivity(), R.drawable.cog, size, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvApp.getApplication().getLogger().Debug("Options...");
            }
        }));


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
                setItem(null);
                if (mGridAdapter.getTotalItems() == 0) mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setTitle(mFolder.getName());

                    }
                }, 250); else focusGrid();
            }
        });
    }

    private void refreshCurrentItem() {
        if (mCurrentItem != null) {
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
            ItemLauncher.launch((BaseRowItem) item, mApplication, getActivity());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            mBackgroundUrl = mCurrentItem.getBackdropImageUrl();
            startBackgroundTimer();
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
                return;
            } else {
                mCurrentItem = (BaseRowItem)item;
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
        Picasso.with(getActivity())
                .load(url)
                //.skipMemoryCache()
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
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
