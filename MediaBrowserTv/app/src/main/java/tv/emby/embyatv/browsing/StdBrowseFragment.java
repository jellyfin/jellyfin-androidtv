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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.EmptyResponse;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.CustomMessage;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.base.IMessageListener;
import tv.emby.embyatv.itemhandling.AudioQueueItem;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemLauncher;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.presentation.CardPresenter;
import tv.emby.embyatv.presentation.PositionableListRowPresenter;
import tv.emby.embyatv.presentation.ThemeManager;
import tv.emby.embyatv.querying.QueryType;
import tv.emby.embyatv.querying.ViewQuery;
import tv.emby.embyatv.ui.ClockUserView;
import tv.emby.embyatv.ui.ItemPanel;
import tv.emby.embyatv.util.KeyProcessor;
import tv.emby.embyatv.util.Utils;

public class StdBrowseFragment extends BrowseFragment implements IRowLoader {
    private static final String TAG = "StdBrowseFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 100;

    protected String MainTitle;
    protected boolean ShowBadge = true;
    protected boolean ShowInfoPanel = true;
    protected boolean ShowFanart = false;
    protected TvApp mApplication;
    protected BaseActivity mActivity;
    protected BaseRowItem mCurrentItem;
    protected ListRow mCurrentRow;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    protected ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private SimpleTarget<Bitmap> mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    CardPresenter mCardPresenter;

    protected boolean justLoaded = true;

    private ItemPanel mItemPanel;
    private Animation fadeInPanel;
    private Animation fadeOutPanel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApplication = TvApp.getApplication();
        if (getActivity() instanceof BaseActivity) mActivity = (BaseActivity)getActivity();

        prepareBackgroundManager();

        setupUIElements();

        setupQueries(this);

        setupEventListeners();
    }

    protected void setupQueries(IRowLoader rowLoader) {
        rowLoader.loadRows(mRows);
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

        // set fastLane (or headers) background color
        setBrandColor(ThemeManager.getBrandColor());

        // set info panel option
        ShowInfoPanel = mApplication.getPrefs().getBoolean("pref_enable_info_panel", true);
        ShowFanart = mApplication.getPrefs().getBoolean("pref_show_backdrop", true);

        //React to deletion
        if (getActivity() != null && !getActivity().isFinishing() && mCurrentRow != null && mCurrentItem != null && mCurrentItem.getItemId() != null && mCurrentItem.getItemId().equals(TvApp.getApplication().getLastDeletedItemId())) {
            ((ItemRowAdapter)mCurrentRow.getAdapter()).remove(mCurrentItem);
            TvApp.getApplication().setLastDeletedItemId(null);
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mRowsAdapter != null) {
                refreshCurrentItem();
                refreshRows();
            }

        } else {
            justLoaded = false;
        }
    }

    protected void refreshRows() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActivity.isFinishing()) return;
                for (int i = 0; i < mRowsAdapter.size(); i++) {
                    if (mRowsAdapter.get(i) instanceof ListRow) {
                        if (((ListRow) mRowsAdapter.get(i)).getAdapter() instanceof ItemRowAdapter && !mActivity.isFinishing()) {
                            ((ItemRowAdapter) ((ListRow) mRowsAdapter.get(i)).getAdapter()).ReRetrieveIfNeeded();
                        }
                    }
                }
            }
        },1500);

    }

    public void loadRows(List<BrowseRowDef> rows) {

        mRowsAdapter = new ArrayObjectAdapter(new PositionableListRowPresenter());
        mCardPresenter = new CardPresenter();

        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText());
            ItemRowAdapter rowAdapter;
            switch (def.getQueryType()) {
                case NextUp:
                    rowAdapter = new ItemRowAdapter(def.getNextUpQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case LatestItems:
                    rowAdapter = new ItemRowAdapter(def.getLatestItemsQuery(), true, mCardPresenter, mRowsAdapter);
                    break;
                case Season:
                    rowAdapter = new ItemRowAdapter(def.getSeasonQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Upcoming:
                    rowAdapter = new ItemRowAdapter(def.getUpcomingQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case Views:
                    rowAdapter = new ItemRowAdapter(new ViewQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case SimilarSeries:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, mRowsAdapter);
                    break;
                case SimilarMovies:
                    rowAdapter = new ItemRowAdapter(def.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, mRowsAdapter);
                    break;
                case Persons:
                    rowAdapter = new ItemRowAdapter(def.getPersonsQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvChannel:
                    rowAdapter = new ItemRowAdapter(def.getTvChannelQuery(), 40, mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvProgram:
                    rowAdapter = new ItemRowAdapter(def.getProgramQuery(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvRecording:
                    rowAdapter = new ItemRowAdapter(def.getRecordingQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
                case LiveTvRecordingGroup:
                    rowAdapter = new ItemRowAdapter(def.getRecordingGroupQuery(), mCardPresenter, mRowsAdapter);
                    break;
                default:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), def.getPreferParentThumb(), def.isStaticHeight(), mCardPresenter, mRowsAdapter, def.getQueryType());
                    break;
            }

            rowAdapter.setReRetrieveTriggers(def.getChangeTriggers());

            ListRow row = new ListRow(header, rowAdapter);
            mRowsAdapter.add(row);
            rowAdapter.setRow(row);
            rowAdapter.Retrieve();
        }

        addAdditionalRows(mRowsAdapter);

        setAdapter(mRowsAdapter);

    }

    protected void addAdditionalRows(ArrayObjectAdapter rowAdapter) {

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
        if (ShowBadge) setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.logob400));
        setTitle(MainTitle); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // move the badge/title to the left to make way for our clock/user bug
        ImageView badge = (ImageView) getActivity().findViewById(R.id.title_badge);
        if (badge != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) badge.getLayoutParams();
            lp.rightMargin = Utils.convertDpToPixel(getActivity(), 120);
            lp.width = Utils.convertDpToPixel(getActivity(), 250);
            badge.setLayoutParams(lp);
        }
        TextView title = (TextView) getActivity().findViewById(R.id.title_text);
        if (title != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) title.getLayoutParams();
            lp.rightMargin = Utils.convertDpToPixel(getActivity(), 120);
            lp.width = Utils.convertDpToPixel(getActivity(), 250);
            title.setLayoutParams(lp);
        }

        ViewGroup root = (ViewGroup) getActivity().findViewById(android.R.id.content);

        // add item panel
        mItemPanel = new ItemPanel(getActivity());
        FrameLayout.LayoutParams panelLayout = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.convertDpToPixel(TvApp.getApplication(), 145));
        panelLayout.gravity = Gravity.BOTTOM;
        panelLayout.bottomMargin = -10;
        mItemPanel.setLayoutParams(panelLayout);
        root.addView(mItemPanel);
        mItemPanel.setVisibility(View.INVISIBLE);

        // and add the clock element
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ClockUserView clock = new ClockUserView(getActivity());
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.rightMargin = Utils.convertDpToPixel(getActivity(), 40);
        layoutParams.topMargin = Utils.convertDpToPixel(getActivity(), 20);
        clock.setLayoutParams(layoutParams);
        root.addView(clock);

        // load item panel animation
        fadeInPanel = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_in);
        fadeInPanel.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mItemPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeOutPanel = AnimationUtils.loadAnimation(mActivity, R.anim.abc_fade_out);
        fadeOutPanel.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mItemPanel.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));

    }

    private Runnable showItemPanel = new Runnable() {
        @Override
        public void run() {
            if (mCurrentItem != null && !mCurrentItem.isFolder() && (mCurrentItem.getRuntimeTicks() > 0 ||  (mCurrentItem.getSummary() != null && mCurrentItem.getSummary().length() > 0))) {
                mItemPanel.setItem(mCurrentItem);
                mItemPanel.startAnimation(fadeInPanel);
                mHandler.removeCallbacks(hideItemPanel);
                mHandler.postDelayed(hideItemPanel, 20000);
            } else {
                mItemPanel.setVisibility(View.INVISIBLE);
            }
        }
    };

    private Runnable hideItemPanel = new Runnable() {
        @Override
        public void run() {
            mItemPanel.startAnimation(fadeOutPanel);
        }
    };

    protected void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                TvApp.getApplication().showSearch(getActivity(), false);
            }
        });

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

    private void refreshCurrentItem() {
        if (mCurrentItem != null && !mCurrentItem.getType().equals("UserView") && !mCurrentItem.getType().equals("CollectionFolder")) {
            TvApp.getApplication().getLogger().Debug("Refresh item "+mCurrentItem.getFullName());
            mCurrentItem.refresh(new EmptyResponse() {
                @Override
                public void onResponse() {
                    ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)mCurrentRow).getAdapter();
                    adapter.notifyArrayItemRangeChanged(adapter.indexOf(mCurrentItem), 1);
                }
            });

        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem) item, (ItemRowAdapter) ((ListRow)row).getAdapter(), ((BaseRowItem)item).getIndex(), getActivity());
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (ShowInfoPanel) {
                // cancel any delayed showing and hide item panel
                mHandler.removeCallbacks(showItemPanel);
                mItemPanel.setVisibility(View.INVISIBLE);
            }

            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                mHandler.removeCallbacks(hideItemPanel);
                //fill in default background
                mBackgroundUrl = null;
                startBackgroundTimer();
                return;
            } else {
                mCurrentItem = (BaseRowItem)item;
                if (!isShowingHeaders() && ShowInfoPanel && !(item instanceof AudioQueueItem)) {
                    // delay show the item panel
                    mHandler.postDelayed(showItemPanel, 1000);
                }
            }

            mCurrentRow = (ListRow) row;
            BaseRowItem rowItem = (BaseRowItem) item;

            //mApplication.getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            if (ShowFanart) {
                mBackgroundUrl = rowItem.getBackdropImageUrl();
                startBackgroundTimer();
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
