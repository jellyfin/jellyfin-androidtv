package org.jellyfin.androidtv.ui.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.FocusHighlightHelper;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.ShadowOverlayContainer;
import androidx.recyclerview.widget.RecyclerView;

import org.jellyfin.androidtv.databinding.HorizontalGridBinding;

import timber.log.Timber;

/**
 * A presenter that renders objects in a horizontal grid.
 */
public class HorizontalGridPresenter extends Presenter {
    public class ViewHolder extends Presenter.ViewHolder {
        final ItemBridgeAdapter mItemBridgeAdapter = new ItemBridgeAdapter();
        final HorizontalGridView mGridView;
        boolean mInitialized;

        public ViewHolder(HorizontalGridView view) {
            super(view);
            mGridView = view;
        }

        public HorizontalGridView getGridView() {
            return mGridView;
        }
    }

    private int mNumRows = -1;
    private int mZoomFactor;
    private boolean mShadowEnabled = true;
    private OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private boolean mRoundedCornersEnabled = true;

    public HorizontalGridPresenter() {
        this(FocusHighlight.ZOOM_FACTOR_LARGE);
    }

    public HorizontalGridPresenter(int zoomFactor) {
        mZoomFactor = zoomFactor;
    }

    public void setPosition(int ndx) {
        if (mViewHolder != null) mViewHolder.getGridView().setSelectedPosition(ndx);
    }

    /**
     * Sets the number of rows in the grid.
     */
    public void setNumberOfRows(int numRows) {
        if (numRows < 0) {
            throw new IllegalArgumentException("Invalid number of rows");
        }
        if (mNumRows != numRows) {
            mNumRows = numRows;
        }
    }

    /**
     * Returns the number of rows in the grid.
     */
    public int getNumberOfRows() {
        return mNumRows;
    }

    /**
     * Enable or disable child shadow.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final void setShadowEnabled(boolean enabled) {
        mShadowEnabled = enabled;
    }

    /**
     * Returns true if child shadow is enabled.
     * This is not only for enable/disable default shadow implementation but also subclass must
     * respect this flag.
     */
    public final boolean getShadowEnabled() {
        return mShadowEnabled;
    }

    /**
     * Returns true if opticalBounds is supported (SDK >= 18) so that default shadow
     * is applied to each individual child of {@link HorizontalGridView}.
     * Subclass may return false to disable.
     */
    public boolean isUsingDefaultShadow() {
        return ShadowOverlayContainer.supportsShadow();
    }

    /**
     * Enables or disabled rounded corners on children of this row.
     * Supported on Android SDK >= L.
     */
    public final void enableChildRoundedCorners(boolean enable) {
        mRoundedCornersEnabled = enable;
    }

    /**
     * Returns true if rounded corners are enabled for children of this row.
     */
    public final boolean areChildRoundedCornersEnabled() {
        return mRoundedCornersEnabled;
    }

    /**
     * Returns true if SDK >= L, where Z shadow is enabled so that Z order is enabled
     * on each child of vertical grid.   If subclass returns false in isUsingDefaultShadow()
     * and does not use Z-shadow on SDK >= L, it should override isUsingZOrder() return false.
     */
    public boolean isUsingZOrder() {
        return false;
    }

    final boolean needsDefaultShadow() {
        return isUsingDefaultShadow() && getShadowEnabled();
    }

    @Override
    public final ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder vh = createGridViewHolder(parent);
        vh.mInitialized = false;
        initializeGridViewHolder(vh);
        if (!vh.mInitialized) {
            throw new RuntimeException("super.initializeGridViewHolder() must be called");
        }
        return vh;
    }

    /**
     * Subclass may override this to inflate a different layout.
     */
    protected ViewHolder createGridViewHolder(ViewGroup parent) {
        HorizontalGridBinding binding = HorizontalGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding.horizontalGrid);
    }

    private ItemBridgeAdapter.Wrapper mWrapper = new ItemBridgeAdapter.Wrapper() {
        @Override
        public View createWrapper(View root) {
            ShadowOverlayContainer wrapper = new ShadowOverlayContainer(root.getContext());
            wrapper.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            wrapper.initialize(needsDefaultShadow(), true, areChildRoundedCornersEnabled());
            return wrapper;
        }
        @Override
        public void wrap(View wrapper, View wrapped) {
            ((ShadowOverlayContainer) wrapper).wrap(wrapped);
        }
    };

    /**
     * Called after a {@link HorizontalGridPresenter.ViewHolder} is created.
     * Subclasses may override this method and start by calling
     * super.initializeGridViewHolder(ViewHolder).
     *
     * @param vh The ViewHolder to initialize for the vertical grid.
     */
    protected void initializeGridViewHolder(ViewHolder vh) {
        if (mNumRows == -1) {
            throw new IllegalStateException("Number of rows must be set");
        }
        Timber.d("mNumRows %s", mNumRows);
        vh.getGridView().setNumRows(mNumRows);
        vh.mInitialized = true;

        vh.mItemBridgeAdapter.setWrapper(mWrapper);
        if (needsDefaultShadow() || areChildRoundedCornersEnabled()) {
            ShadowOverlayContainer.prepareParentForShadow(vh.getGridView());
            ((ViewGroup) vh.view).setClipChildren(false);
        }
        vh.getGridView().setFocusDrawingOrderEnabled(!isUsingZOrder());
        FocusHighlightHelper.setupBrowseItemFocusHighlight(vh.mItemBridgeAdapter,
                mZoomFactor, true);

        final ViewHolder gridViewHolder = vh;
        vh.getGridView().setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
                if (child != null) {
                    selectChildView(gridViewHolder, child.itemView);
                }
            }
        });

        vh.mItemBridgeAdapter.setAdapterListener(new ItemBridgeAdapter.AdapterListener() {
            @Override
            public void onBind(final ItemBridgeAdapter.ViewHolder itemViewHolder) {
                // Only when having an OnItemClickListener, we attach the OnClickListener.
                if (getOnItemViewClickedListener() != null) {
                    final View itemView = itemViewHolder.getViewHolder().view;
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (getOnItemViewClickedListener() != null) {
                                // Row is always null
                                getOnItemViewClickedListener().onItemClicked(
                                        itemViewHolder.getViewHolder(), itemViewHolder.getItem(), null, null);
                            }
                        }
                    });
                }
            }

            @Override
            public void onUnbind(ItemBridgeAdapter.ViewHolder viewHolder) {
                if (getOnItemViewClickedListener() != null) {
                    viewHolder.getViewHolder().view.setOnClickListener(null);
                }
            }

            @Override
            public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder viewHolder) {
                viewHolder.itemView.setActivated(true);
            }
        });
    }

    ViewHolder mViewHolder;

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        Timber.d("onBindViewHolder %s", item);
        mViewHolder = (ViewHolder) viewHolder;
        mViewHolder.mItemBridgeAdapter.setAdapter((ObjectAdapter) item);
        mViewHolder.getGridView().setAdapter(mViewHolder.mItemBridgeAdapter);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Timber.d("onUnbindViewHolder");
        ViewHolder vh = (ViewHolder) viewHolder;
        vh.mItemBridgeAdapter.setAdapter(null);
        vh.getGridView().setAdapter(null);
    }

    /**
     * Sets the item selected listener.
     * Since this is a grid the row parameter is always null.
     */
    public final void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mOnItemViewSelectedListener = listener;
    }

    /**
     * Returns the item selected listener.
     */
    public final OnItemViewSelectedListener getOnItemViewSelectedListener() {
        return mOnItemViewSelectedListener;
    }

    /**
     * Sets the item clicked listener.
     * OnItemViewClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general, developer should choose one of the listeners but not both.
     */
    public final void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
    }

    /**
     * Returns the item clicked listener.
     */
    public final OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    private void selectChildView(ViewHolder vh, View view) {
        if (getOnItemViewSelectedListener() != null) {
            ItemBridgeAdapter.ViewHolder ibh = (view == null) ? null :
                    (ItemBridgeAdapter.ViewHolder) vh.getGridView().getChildViewHolder(view);
            if (ibh == null) {
                getOnItemViewSelectedListener().onItemSelected(null, null, null, null);
            } else {
                getOnItemViewSelectedListener().onItemSelected(ibh.getViewHolder(), ibh.getItem(), null, null);
            }
        }
    }
}

