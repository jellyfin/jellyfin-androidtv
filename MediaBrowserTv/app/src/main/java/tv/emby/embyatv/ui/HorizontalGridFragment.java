package tv.emby.embyatv.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.presentation.HorizontalGridPresenter;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 8/17/2015.
 */
public class HorizontalGridFragment extends Fragment {
    private static final String TAG = "HorizontalGridFragment";
    private static boolean DEBUG = false;

    private TextView mTitleView;
    private LinearLayout mInfoRow;
    private RelativeLayout mToolBar;
    private FrameLayout mGridArea;
    private ObjectAdapter mAdapter;
    private HorizontalGridPresenter mGridPresenter;
    private HorizontalGridPresenter.ViewHolder mGridViewHolder;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private int mSelectedPosition = -1;

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
    public void setAdapter(ObjectAdapter adapter) {
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
        return Utils.convertDpToPixel(TvApp.getApplication(), 370);
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

    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridViewHolder.getGridView().getSelectedPosition();
                    if (DEBUG) Log.v(TAG, "row selected position " + position);
                    onRowSelected(position);
                    if (mOnItemViewSelectedListener != null) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.horizontal_grid_browse,
                container, false);

        mTitleView = (TextView) root.findViewById(R.id.title);
        mTitleView.setTypeface(TvApp.getApplication().getDefaultFont());
        mInfoRow = (LinearLayout) root.findViewById(R.id.infoRow);
        mToolBar = (RelativeLayout) root.findViewById(R.id.toolBar);
        mGridArea = (FrameLayout) root.findViewById(R.id.rowsFragment);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ViewGroup gridDock = (ViewGroup) view.findViewById(R.id.rowsFragment);
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock);
        gridDock.addView(mGridViewHolder.view);

        updateAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGridViewHolder.getGridView().requestFocus();
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
