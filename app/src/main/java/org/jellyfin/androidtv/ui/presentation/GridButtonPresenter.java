package org.jellyfin.androidtv.ui.presentation;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.GridButton;

public class GridButtonPresenter extends Presenter {

    private boolean mShowInfo = true;
    private int mCardWidth = 220;
    private int mCardHeight = 220;

    public GridButtonPresenter() {
        super();
    }

    public GridButtonPresenter(boolean showinfo, int width, int height) {
        this();
        mShowInfo = showinfo;
        mCardWidth = width;
        mCardHeight = height;
    }

    static class ViewHolder extends Presenter.ViewHolder {
        private GridButton mItem;
        private int cardWidth;
        private int cardHeight;
        private MyImageCardView mCardView;
        private Drawable mDefaultCardImage;

        public ViewHolder(View view) {
            super(view);

            mCardView = (MyImageCardView) view;
            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_settings);
        }

        public void setItem(GridButton m, int width, int height) {
            mItem = m;
            cardWidth = width;
            cardHeight = height;
            mCardView.setMainImageDimensions(width, height);
        }

        public GridButton getItem() {
            return mItem;
        }

        protected void updateCardViewImage(@DrawableRes int image) {
            mCardView.getMainImageView().setImageResource(image);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        MyImageCardView cardView = new MyImageCardView(parent.getContext(), mShowInfo);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = parent.getContext().getTheme();
        theme.resolveAttribute(R.attr.cardViewBackground, typedValue, true);
        @ColorInt int color = typedValue.data;
        cardView.setBackgroundColor(color);

        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof GridButton)) return;
        GridButton gridItem = (GridButton) item;

        ViewHolder vh = (ViewHolder) viewHolder;

        vh.setItem(gridItem, mCardWidth, mCardHeight);
        vh.mCardView.setTitleText(gridItem.getText());
        vh.mCardView.setOverlayText(gridItem.getText());
        vh.updateCardViewImage(gridItem.getImageIndex());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }
}
