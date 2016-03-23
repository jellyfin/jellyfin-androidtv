package tv.emby.embyatv.ui;

import android.app.Fragment;
import android.content.ClipData;
import android.os.Bundle;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import mediabrowser.model.entities.SortOrder;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.itemhandling.ItemRowAdapter;
import tv.emby.embyatv.model.FilterOptions;
import tv.emby.embyatv.presentation.HorizontalGridPresenter;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 8/17/2015.
 */
public class HorizontalGridFragment extends Fragment {
    private static final String TAG = "HorizontalGridFragment";
    private static boolean DEBUG = false;

    protected TextView mTitleView;
    private TextView mStatusText;
    private TextView mCounter;
    protected FrameLayout mSpinner;
    protected ViewGroup mGridDock;
    protected LinearLayout mInfoRow;
    protected LinearLayout mToolBar;
    private ItemRowAdapter mAdapter;
    private HorizontalGridPresenter mGridPresenter;
    private HorizontalGridPresenter.ViewHolder mGridViewHolder;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private int mSelectedPosition = -1;

    protected int SMALL_CARD = Utils.convertDpToPixel(TvApp.getApplication(), 116);
    protected int MED_CARD = Utils.convertDpToPixel(TvApp.getApplication(), 175);
    protected int LARGE_CARD = Utils.convertDpToPixel(TvApp.getApplication(), 210);
    protected int SMALL_BANNER = Utils.convertDpToPixel(TvApp.getApplication(), 58);
    protected int MED_BANNER = Utils.convertDpToPixel(TvApp.getApplication(), 88);
    protected int LARGE_BANNER = Utils.convertDpToPixel(TvApp.getApplication(), 105);

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(HorizontalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        mGridPresenter = gridPresenter;
        mGridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        if (mOnItemViewClickedListener != null) {
            mGridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
    }

    /**
     * Returns the grid presenter.
     */
    public HorizontalGridPresenter getGridPresenter() {
        return mGridPresenter;
    }

    /**
     * Sets the object adapter for the fragment.
     */
    public void setAdapter(ItemRowAdapter adapter) {
        mAdapter = adapter;
        updateAdapter();
    }

    /**
     * Returns the object adapter.
     */
    public ObjectAdapter getAdapter() {
        return mAdapter;
    }

    public int getGridHeight() {
        return Utils.convertDpToPixel(TvApp.getApplication(), 400);
    }

    public void setItem(BaseRowItem item) {
        if (item != null) {
            mTitleView.setText(item.getFullName());
            InfoLayoutHelper.addInfoRow(getActivity(), item, mInfoRow, true, true);
        } else {
            mTitleView.setText("");
            mInfoRow.removeAllViews();
        }
    }

    public class SortOption {
        public String name;
        public String value;
        public SortOrder order;

        public SortOption(String name, String value, SortOrder order) {
            this.name = name;
            this.value = value;
            this.order = order;
        }
    }

    protected Map<Integer, SortOption> sortOptions = new HashMap<>();
    {
        sortOptions.put(0, new SortOption(TvApp.getApplication().getString(R.string.lbl_name), "SortName", SortOrder.Ascending));
        sortOptions.put(1, new SortOption(TvApp.getApplication().getString(R.string.lbl_date_added), "DateLastContentAdded", SortOrder.Descending));
        sortOptions.put(2, new SortOption(TvApp.getApplication().getString(R.string.lbl_premier_date), "PremiereDate", SortOrder.Descending));
        sortOptions.put(3,new SortOption(TvApp.getApplication().getString(R.string.lbl_rating), "OfficialRating", SortOrder.Ascending));
        sortOptions.put(4,new SortOption(TvApp.getApplication().getString(R.string.lbl_critic_rating), "CriticRating", SortOrder.Descending));
        sortOptions.put(5,new SortOption(TvApp.getApplication().getString(R.string.lbl_last_played), "DatePlayed", SortOrder.Descending));
    }

    protected String getSortFriendlyName(String value) {
        return getSortOption(value).name;
    }

    protected SortOption getSortOption(String value) {
        for (Integer key : sortOptions.keySet()) {
            SortOption option = sortOptions.get(key);
            if (option.value.equals(value)) return option;
        }

        return new SortOption("Unknown","",SortOrder.Ascending);
    }

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    public void setStatusText(String folderName) {
        String text = TvApp.getApplication().getResources().getString(R.string.lbl_showing) + " ";
        FilterOptions filters = mAdapter.getFilters();
        if (filters == null || (!filters.isFavoriteOnly() && !filters.isUnwatchedOnly())) {
            text += TvApp.getApplication().getResources().getString(R.string.lbl_all_items);
        } else {
            text += (filters.isUnwatchedOnly() ? TvApp.getApplication().getResources().getString(R.string.lbl_unwatched) : "") + " " +
                    (filters.isFavoriteOnly() ? TvApp.getApplication().getResources().getString(R.string.lbl_favorites) : "");
        }

        if (mAdapter.getStartLetter() != null) {
            text += " " + TvApp.getApplication().getResources().getString(R.string.lbl_starting_with) + " " + mAdapter.getStartLetter();
        }

        text += " " + TvApp.getApplication().getString(R.string.lbl_from) + " '" + folderName + "' " + TvApp.getApplication().getString(R.string.lbl_sorted_by) + " " + getSortFriendlyName(mAdapter.getSortBy());

        mStatusText.setText(text);
    }

    public LinearLayout getToolBar() { return mToolBar; }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridViewHolder.getGridView().getSelectedPosition();
                    if (DEBUG) Log.v(TAG, "row selected position " + position);
                    onRowSelected(position);
                    if (mOnItemViewSelectedListener != null && position >= 0) {
                        mOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                                rowViewHolder, row);
                    }
                }
            };

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mOnItemViewSelectedListener = listener;
    }

    private void onRowSelected(int position) {
        if (position != mSelectedPosition) {
            mSelectedPosition = position;
        }
        // Update the counter
        updateCounter(position+1);
    }

    public void updateCounter(int position) {
        if (mAdapter != null) {
            mCounter.setText((position)+" | "+ mAdapter.getTotalItems());
        }

    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
        if (mGridPresenter != null) {
            mGridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    public void showSpinner() {
        if (getActivity() == null || getActivity().isFinishing() || mSpinner == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpinner.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideSpinner() {
        if (getActivity() == null || getActivity().isFinishing() || mSpinner == null) return;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpinner.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.horizontal_grid_browse,
                container, false);

        mTitleView = (TextView) root.findViewById(R.id.title);
        mTitleView.setTypeface(TvApp.getApplication().getDefaultFont());
        mStatusText = (TextView) root.findViewById(R.id.statusText);
        mStatusText.setTypeface(TvApp.getApplication().getDefaultFont());
        mInfoRow = (LinearLayout) root.findViewById(R.id.infoRow);
        mToolBar = (LinearLayout) root.findViewById(R.id.toolBar);
        mCounter = (TextView) root.findViewById(R.id.counter);
        mCounter.setTypeface(TvApp.getApplication().getDefaultFont());
        mSpinner = (FrameLayout) root.findViewById(R.id.spinner);
        mGridDock = (ViewGroup) root.findViewById(R.id.rowsFragment);

        // Hide the description because we don't have room for it
        ((NowPlayingBug)root.findViewById(R.id.npBug)).showDescription(false);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        createGrid();
    }

    protected void createGrid() {
        mGridViewHolder = mGridPresenter.onCreateViewHolder(mGridDock);
        mGridViewHolder.getGridView().setFocusable(true);
        mGridDock.removeAllViews();
        mGridViewHolder.getGridView().setGravity(Gravity.CENTER_VERTICAL);
        mGridDock.addView(mGridViewHolder.view);

        updateAdapter();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void focusGrid() {
        if (mGridViewHolder != null && mGridViewHolder.getGridView() != null) mGridViewHolder.getGridView().requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridViewHolder = null;
    }

    /**
     * Sets the selected item position.
     */
    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        if(mGridViewHolder != null && mGridViewHolder.getGridView().getAdapter() != null) {
            mGridViewHolder.getGridView().setSelectedPositionSmooth(position);
        }
    }

    private void updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridViewHolder.getGridView().setSelectedPosition(mSelectedPosition);
            }
        }
    }
}
