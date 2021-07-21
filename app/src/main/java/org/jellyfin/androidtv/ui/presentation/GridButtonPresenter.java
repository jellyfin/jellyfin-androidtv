package org.jellyfin.androidtv.ui.presentation;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.ui.card.LegacyImageCardView;

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

    class ViewHolder extends Presenter.ViewHolder {
        private GridButton gridButton;
        private final LegacyImageCardView cardView;

        public ViewHolder(View view) {
            super(view);
            cardView = (LegacyImageCardView) view;
        }

        public void setItem(GridButton m, int width, int height) {
            gridButton = m;
            cardView.setMainImageDimensions(width, height);
            if (gridButton.getImageUrl() == null) {
                cardView.getMainImageView().setImageResource(gridButton.getImageRes());
            } else {
                Glide.with(cardView.getContext())
                        .load(gridButton.getImageUrl())
                        .error(gridButton.getImageRes())
                        .into(cardView.getMainImageView());
            }
        }

        public GridButton getItem() {
            return gridButton;
        }

        protected void updateCardViewImage(@DrawableRes int image) {
            cardView.getMainImageView().setImageResource(image);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        LegacyImageCardView cardView = new LegacyImageCardView(parent.getContext(), mShowInfo);
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
        vh.cardView.setTitleText(gridItem.getText());
        vh.cardView.setOverlayText(gridItem.getText());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }
}
