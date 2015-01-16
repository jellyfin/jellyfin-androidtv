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

package tv.mediabrowser.mediabrowsertv;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;

interface IRowLoader {
    void loadRows(List<BrowseRowDef> rows);
}

public class StdBrowseFragment extends BrowseFragment implements IRowLoader {
    private static final String TAG = "StdBrowseFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 100;

    protected String MainTitle;
    protected boolean ShowBadge = true;
    protected ApiClient mApiClient;
    protected TvApp mApplication;
    protected CompositeClickedListener mClickedListener = new CompositeClickedListener();
    protected CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private String mBackgroundUrl;
    protected ArrayList<BrowseRowDef> mRows = new ArrayList<>();
    CardPresenter mCardPresenter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        mApplication = TvApp.getApplication();

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
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    public void loadRows(List<BrowseRowDef> rows) {

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mCardPresenter = new CardPresenter();

        for (BrowseRowDef def : rows) {
            HeaderItem header = new HeaderItem(def.getHeaderText(), null);
            ItemRowAdapter rowAdapter;
            switch (def.getQueryType()) {
                case NextUp:
                    rowAdapter = new ItemRowAdapter(def.getNextUpQuery(), mCardPresenter, mRowsAdapter);
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
                default:
                    rowAdapter = new ItemRowAdapter(def.getQuery(), def.getChunkSize(), mCardPresenter, mRowsAdapter);
                    break;
            }

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

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.moviebg);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void setupUIElements() {
        if (ShowBadge) setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.mblogo));
        setTitle(MainTitle); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    protected void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "In-app search not implemented", Toast.LENGTH_LONG)
                        .show();
            }
        });

        setOnItemViewClickedListener(mClickedListener);
        mClickedListener.registerListener(new ItemViewClickedListener());

        setOnItemViewSelectedListener(mSelectedListener);
        mSelectedListener.registerListener(new ItemViewSelectedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            ItemLauncher.launch((BaseRowItem)item, mApplication,getActivity(),itemViewHolder);
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem)) {
                //fill in default background
                mBackgroundUrl = null;
                startBackgroundTimer();
                return;
            }

            BaseRowItem rowItem = (BaseRowItem) item;

            mApplication.getLogger().Debug("Selected Item "+rowItem.getIndex() + " type: "+ (rowItem.getItemType().equals(BaseRowItem.ItemType.BaseItem) ? rowItem.getBaseItem().getType() : "other"));
            ItemRowAdapter adapter = (ItemRowAdapter) ((ListRow)row).getAdapter();
            adapter.loadMoreItemsIfNeeded(rowItem.getIndex());

            mBackgroundUrl = rowItem.getBackdropImageUrl();
            startBackgroundTimer();

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

    protected class CompositeClickedListener implements OnItemViewClickedListener {
        private List<OnItemViewClickedListener> registeredListeners = new ArrayList<>();

        public void registerListener (OnItemViewClickedListener listener) {
            registeredListeners.add(listener);
        }

        public void unRegisterListener (OnItemViewClickedListener listener) {
            registeredListeners.remove(listener);
        }

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            for (OnItemViewClickedListener listener : registeredListeners) {
                listener.onItemClicked(itemViewHolder, item, rowViewHolder, row);
            }
        }
    }

    protected class CompositeSelectedListener implements OnItemViewSelectedListener {
        private List<OnItemViewSelectedListener> registeredListeners = new ArrayList<>();

        public void registerListener (OnItemViewSelectedListener listener) {
            registeredListeners.add(listener);
        }

        public void unRegisterListener (OnItemViewSelectedListener listener) {
            registeredListeners.remove(listener);
        }


        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            for (OnItemViewSelectedListener listener : registeredListeners) {
                listener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
            }
        }
    }
}
