package tv.mediabrowser.mediabrowsertv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import mediabrowser.model.dto.BaseItemDto;

public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static Context mContext;
    private TvApp application;

    public CardPresenter() {
        super();
        application = TvApp.getApplication();

    }

    static class ViewHolder extends Presenter.ViewHolder {
        private int cardWidth = 230;
        private int cardHeight = 300;
        private BaseRowItem mItem;
        private MyImageCardView mCardView;
        private Drawable mDefaultCardImage;
        private PicassoImageCardViewTarget mImageCardViewTarget;

        public ViewHolder(View view) {
            super(view);
            mCardView = (MyImageCardView) view;

            mImageCardViewTarget = new PicassoImageCardViewTarget(mCardView);
            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.video);
        }

        public void setItem(BaseRowItem m) {
            mItem = m;
            switch (mItem.getItemType()) {

                case BaseItem:
                    BaseItemDto itemDto = mItem.getBaseItem();
                    cardWidth = (int)((Utils.getImageAspectRatio(itemDto)) * cardHeight);
                    if (cardWidth < 10) cardWidth = 230;  //Guard against zero size images causing picasso to barf
                    //TvApp.getApplication().getLogger().Debug("**** Image width: "+ cardWidth + " Aspect: " + Utils.getImageAspectRatio(itemDto) + " Item: "+itemDto.getName());
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    switch (itemDto.getType()) {
                        case "Audio":
                        case "MusicAlbum":
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.audio);
                            break;
                        case "Person":
                        case "MusicArtist":
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.person);
                            break;
                        case "Season":
                        case "Series":
                        case "Episode":
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.tv);
                            break;
                        case "Folder":
                        case "CollectionFolder":
                        case "MovieGenreFolder":
                        case "MusicGenreFolder":
                        case "MovieGenre":
                        case "Genre":
                        case "MusicGenre":
                        case "UserView":
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.folder);
                            break;
                        default:
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.video);
                            break;

                    }
                    break;
                case Server:
                    cardWidth = (int)(.777777777 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.server);
                case Person:
                    cardWidth = (int)(.777777777 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.person);
                    break;
                case User:
                    cardWidth = (int)(.777777777 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.person);
                    break;
                case Chapter:
                    break;
                case SearchHint:
                    switch (mItem.getSearchHint().getType()) {
                        case "Episode":
                            cardWidth = (int)(1.77777777 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.tv);
                            break;
                        case "Person":
                            cardWidth = (int)(.777777777 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.person);
                            break;
                        default:
                            cardWidth = (int)(.777777777 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = mContext.getResources().getDrawable(R.drawable.video);
                            break;
                    }
            }
        }

        public BaseRowItem getItem() {
            return mItem;
        }

        public ImageCardView getCardView() {
            return mCardView;
        }

        protected void updateCardViewImage(String url) {
            Picasso.with(mContext)
                    .load(url)
                    .resize(cardWidth, cardHeight)
                    .centerCrop()
                    .error(mDefaultCardImage)
                    .into(mImageCardViewTarget);
        }

        protected void resetCardViewImage() {
            Picasso.with(mContext)
                    .load(Uri.parse("android.resource://tv.mediabrowser.mediabrowsertv/drawable/loading"))
                    .resize(cardWidth, cardHeight)
                    .centerCrop()
                    .error(mDefaultCardImage)
                    .into(mImageCardViewTarget);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        //Log.d(TAG, "onCreateViewHolder");
        mContext = parent.getContext();

        MyImageCardView cardView = new MyImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(mContext.getResources().getColor(R.color.lb_basic_card_info_bg_color));
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof BaseRowItem)) return;
        BaseRowItem rowItem = (BaseRowItem) item;

        ((ViewHolder) viewHolder).setItem(rowItem);

        //Log.d(TAG, "onBindViewHolder");
        ((ViewHolder) viewHolder).mCardView.setTitleText(rowItem.getFullName());
        ((ViewHolder) viewHolder).mCardView.setContentText(rowItem.getSubText());
        Drawable badge = rowItem.getBadgeImage();
        if (badge != null) {
            ((ViewHolder) viewHolder).mCardView.setBadgeImage(badge);

        }

        ((ViewHolder) viewHolder).updateCardViewImage(rowItem.getPrimaryImageUrl());

    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onUnbindViewHolder");
        //Get the image out of there so won't be there if recycled
        ((ViewHolder) viewHolder).resetCardViewImage();
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        //Log.d(TAG, "onViewAttachedToWindow");
    }

    public static class PicassoImageCardViewTarget implements Target {
        private ImageCardView mImageCardView;

        public PicassoImageCardViewTarget(ImageCardView mImageCardView) {
            this.mImageCardView = mImageCardView;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            Drawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            mImageCardView.setMainImage(bitmapDrawable);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            mImageCardView.setMainImage(drawable);
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            // Do nothing, default_background manager has its own transitions
        }
    }

}
