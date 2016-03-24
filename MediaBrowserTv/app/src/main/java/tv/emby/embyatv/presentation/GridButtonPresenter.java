package tv.emby.embyatv.presentation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.ui.GridButton;

public class GridButtonPresenter extends Presenter {

    private boolean mShowInfo = true;
    private int mCardWidth = 220;
    private int mCardHeight = 220;
    private static ViewGroup mViewParent;

    public GridButtonPresenter() { super();}

    public GridButtonPresenter(boolean showinfo, int width, int height) {
        this();
        mShowInfo = showinfo;
        mCardWidth = width;
        mCardHeight = height;
    }

    private static Context getContext() {
        return TvApp.getApplication().getCurrentActivity() != null ? TvApp.getApplication().getCurrentActivity() : mViewParent.getContext();
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

            mDefaultCardImage = TvApp.getApplication().getResources().getDrawable(R.drawable.gears);
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

        public MyImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(int image) {
            Glide.with(getContext())
                    .load(image)
                    .override(cardWidth, cardHeight)
                    .centerCrop()
                    .error(mDefaultCardImage)
                    .into(mCardView.getMainImageView());
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        //Log.d(TAG, "onCreateViewHolder");
        mViewParent = parent;

        MyImageCardView cardView = new MyImageCardView(getContext(), mShowInfo);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(TvApp.getApplication().getResources().getColor(R.color.lb_basic_card_info_bg_color));
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof GridButton)) return;
        GridButton gridItem = (GridButton) item;

        ViewHolder vh = (ViewHolder) viewHolder;

        vh.setItem(gridItem, mCardWidth, mCardHeight);

        //Log.d(TAG, "onBindViewHolder");
        vh.mCardView.setTitleText(gridItem.getText());
        vh.mCardView.setOverlayText(gridItem.getText());
        vh.updateCardViewImage(gridItem.getImageIndex());

    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onUnbindViewHolder");
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onViewAttachedToWindow");
    }

}
