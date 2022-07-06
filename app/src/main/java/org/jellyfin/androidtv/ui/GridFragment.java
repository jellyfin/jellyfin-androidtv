package org.jellyfin.androidtv.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.presentation.HorizontalGridPresenter;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.entities.SortOrder;
import org.jellyfin.apiclient.model.querying.ItemSortBy;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class GridFragment extends Fragment {
    protected TextView mTitleView;
    private TextView mStatusText;
    private TextView mCounter;
    protected ViewGroup mGridDock;
    protected LinearLayout mInfoRow;
    protected LinearLayout mToolBar;
    private ItemRowAdapter mAdapter;
    private Presenter mGridPresenter;
    private Presenter.ViewHolder mGridViewHolder;
    private BaseGridView mGridView;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private BaseGridView.OnKeyInterceptListener mOnKeyInterceptListener;
    private int mSelectedPosition = -1;
    private int mCardHeight;

    protected int SMALL_CARD;
    protected int MED_CARD;
    protected int LARGE_CARD;
    protected int SMALL_BANNER;
    protected int MED_BANNER;
    protected int LARGE_BANNER;
    protected int SMALL_VERTICAL_POSTER;
    protected int MED_VERTICAL_POSTER;
    protected int LARGE_VERTICAL_POSTER;
    protected int SMALL_VERTICAL_SQUARE;
    protected int MED_VERTICAL_SQUARE;
    protected int LARGE_VERTICAL_SQUARE;
    protected int SMALL_VERTICAL_THUMB;
    protected int MED_VERTICAL_THUMB;
    protected int LARGE_VERTICAL_THUMB;
    protected int SMALL_VERTICAL_BANNER;
    protected int MED_VERTICAL_BANNER;
    protected int LARGE_VERTICAL_BANNER;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SMALL_CARD = Utils.convertDpToPixel(getContext(), 116);
        MED_CARD = Utils.convertDpToPixel(getContext(), 175);
        LARGE_CARD = Utils.convertDpToPixel(getContext(), 210);
        SMALL_BANNER = Utils.convertDpToPixel(getContext(), 58);
        MED_BANNER = Utils.convertDpToPixel(getContext(), 88);
        LARGE_BANNER = Utils.convertDpToPixel(getContext(), 105);
        SMALL_VERTICAL_POSTER = Utils.convertDpToPixel(getContext(), 116);
        MED_VERTICAL_POSTER = Utils.convertDpToPixel(getContext(), 171);
        LARGE_VERTICAL_POSTER = Utils.convertDpToPixel(getContext(), 202);
        SMALL_VERTICAL_SQUARE = Utils.convertDpToPixel(getContext(), 114);
        MED_VERTICAL_SQUARE = Utils.convertDpToPixel(getContext(), 163);
        LARGE_VERTICAL_SQUARE = Utils.convertDpToPixel(getContext(), 206);
        SMALL_VERTICAL_THUMB = Utils.convertDpToPixel(getContext(), 116);
        MED_VERTICAL_THUMB = Utils.convertDpToPixel(getContext(), 155);
        LARGE_VERTICAL_THUMB = Utils.convertDpToPixel(getContext(), 210);
        SMALL_VERTICAL_BANNER = Utils.convertDpToPixel(getContext(), 51);
        MED_VERTICAL_BANNER = Utils.convertDpToPixel(getContext(), 77);
        LARGE_VERTICAL_BANNER = Utils.convertDpToPixel(getContext(), 118);

        sortOptions = new HashMap<>();
        {
            sortOptions.put(0, new SortOption(getString(R.string.lbl_name), ItemSortBy.SortName, SortOrder.Ascending));
            sortOptions.put(1, new SortOption(getString(R.string.lbl_date_added), ItemSortBy.DateCreated + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(2, new SortOption(getString(R.string.lbl_premier_date), ItemSortBy.PremiereDate + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(3, new SortOption(getString(R.string.lbl_rating), ItemSortBy.OfficialRating + "," + ItemSortBy.SortName, SortOrder.Ascending));
            sortOptions.put(4, new SortOption(getString(R.string.lbl_community_rating), ItemSortBy.CommunityRating + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(5,new SortOption(getString(R.string.lbl_critic_rating), ItemSortBy.CriticRating + "," + ItemSortBy.SortName, SortOrder.Descending));
            sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.DatePlayed + "," + ItemSortBy.SortName, SortOrder.Descending));
        };
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(HorizontalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        if (mOnItemViewClickedListener != null) {
            gridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
        mGridPresenter = gridPresenter;
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        if (mOnItemViewClickedListener != null) {
            gridPresenter.setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
        mGridPresenter = gridPresenter;
    }

    /**
     * Returns the grid presenter.
     */
    public Presenter getGridPresenter() {
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
        return Utils.convertDpToPixel(requireContext(), 400);
    }

    public void setItem(BaseRowItem item) {
        if (item != null) {
            mTitleView.setText(item.getFullName(requireContext()));
            InfoLayoutHelper.addInfoRow(requireContext(), item, mInfoRow, true, true);
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

    protected Map<Integer, SortOption> sortOptions;

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
        String text = getString(R.string.lbl_showing) + " ";
        FilterOptions filters = mAdapter.getFilters();
        if (filters == null || (!filters.isFavoriteOnly() && !filters.isUnwatchedOnly())) {
            text += getString(R.string.lbl_all_items);
        } else {
            text += (filters.isUnwatchedOnly() ? getString(R.string.lbl_unwatched) : "") + " " +
                    (filters.isFavoriteOnly() ? getString(R.string.lbl_favorites) : "");
        }

        if (mAdapter.getStartLetter() != null) {
            text += " " + getString(R.string.lbl_starting_with) + " " + mAdapter.getStartLetter();
        }

        text += " " + getString(R.string.lbl_from) + " '" + folderName + "' " + getString(R.string.lbl_sorted_by) + " " + getSortFriendlyName(mAdapter.getSortBy());

        mStatusText.setText(text);
    }

    public LinearLayout getToolBar() { return mToolBar; }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridView.getSelectedPosition();
                    Timber.d("row selected position %s", position);
                    onRowSelected(position);
                    if (mOnItemViewSelectedListener != null && position >= 0) {
                        mOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                                rowViewHolder, row);
                    }
                    if (mGridPresenter instanceof VerticalGridPresenter) {
                        if (mCardHeight == SMALL_VERTICAL_BANNER && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns()) {
                            mGridView.setWindowAlignmentOffsetPercent((float) 11.4);
                        } else if (mCardHeight == SMALL_VERTICAL_BANNER && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns() * 2) {
                            mGridView.setWindowAlignmentOffsetPercent((float) 25.95);
                        } else if (mCardHeight == SMALL_VERTICAL_BANNER && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns() * 3) {
                            mGridView.setWindowAlignmentOffsetPercent((float) 40.5);
                        } else if (mCardHeight == MED_VERTICAL_BANNER && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns()) {
                            mGridView.setWindowAlignmentOffsetPercent(15);
                        } else if (mCardHeight == MED_VERTICAL_BANNER && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns() * 2) {
                            mGridView.setWindowAlignmentOffsetPercent(36);
                        } else if (mCardHeight == SMALL_VERTICAL_SQUARE && position >= ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns() && position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns() * 2) {
                            mGridView.setWindowAlignmentOffsetPercent((float) 49.3);
                            mGridView.setWindowAlignmentOffset(0);
                        } else if (position < ((VerticalGridPresenter)mGridPresenter).getNumberOfColumns()) {
                            mGridView.setWindowAlignmentOffsetPercent((float) 5.1);
                            mGridView.setWindowAlignmentOffset((int) Math.round(mCardHeight * 0.5));
                        } else {
                            mGridView.setWindowAlignmentOffsetPercent(50);
                            mGridView.setWindowAlignmentOffset(0);
                        }
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
            if (mGridPresenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter)mGridPresenter).setOnItemViewClickedListener(mOnItemViewClickedListener);
            else if (mGridPresenter instanceof VerticalGridPresenter)
                ((VerticalGridPresenter)mGridPresenter).setOnItemViewClickedListener(mOnItemViewClickedListener);
        }
    }

    public void setOnKeyInterceptListener(BaseGridView.OnKeyInterceptListener listener) {
        mOnKeyInterceptListener = listener;
        if (mGridView != null) {
            mGridView.setOnKeyInterceptListener(listener);
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        HorizontalGridBrowseBinding binding = HorizontalGridBrowseBinding.inflate(inflater, container, false);

        mTitleView = binding.title;
        mStatusText = binding.statusText;
        mInfoRow = binding.infoRow;
        mToolBar = binding.toolBar;
        mCounter = binding.counter;
        mGridDock = binding.rowsFragment;

        // Hide the description because we don't have room for it
        binding.npBug.showDescription(false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        createGrid();
    }

    protected void createGrid() {
        mGridViewHolder = mGridPresenter.onCreateViewHolder(mGridDock);
        if (mGridViewHolder instanceof HorizontalGridPresenter.ViewHolder)
            mGridView = ((HorizontalGridPresenter.ViewHolder)mGridViewHolder).getGridView();
        else if (mGridViewHolder instanceof VerticalGridPresenter.ViewHolder)
            mGridView = ((VerticalGridPresenter.ViewHolder)mGridViewHolder).getGridView();

        mGridView.setFocusable(true);
        mGridView.setOnKeyInterceptListener(mOnKeyInterceptListener);

        mGridDock.removeAllViews();
        mGridView.setGravity(Gravity.CENTER_VERTICAL);
        mGridDock.addView(mGridViewHolder.view);

        updateAdapter();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void focusGrid() {
        if (mGridView != null) mGridView.requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGridView = null;
    }

    private void updateAdapter() {
        if (mGridView != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridView.setSelectedPosition(mSelectedPosition);
            }
        }
    }

    protected void setCardHeight(int height) {
        mCardHeight = height;
    }
}
