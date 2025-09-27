package org.jellyfin.androidtv.ui.presentation;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.Presenter;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.RatingType;
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior;
import org.jellyfin.androidtv.ui.card.LegacyImageCardView;
import org.jellyfin.androidtv.ui.itemhandling.AudioQueueBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.JellyfinImage;
import org.jellyfin.androidtv.util.apiclient.JellyfinImageKt;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.UserItemDataDto;
import org.koin.java.KoinJavaComponent;

import java.time.LocalDateTime;
import java.util.Locale;

import kotlin.Lazy;

public class CardPresenter extends Presenter {
    private int mStaticHeight = 150;
    private ImageType mImageType = ImageType.POSTER;
    private double aspect;
    private boolean mShowInfo = true;
    private boolean isUserView = false;
    private boolean isUniformAspect = false;
    private final Lazy<ImageHelper> imageHelper = KoinJavaComponent.<ImageHelper>inject(ImageHelper.class);

    public CardPresenter() {
        super();
    }

    public CardPresenter(boolean showInfo) {
        this();
        mShowInfo = showInfo;
    }

    public CardPresenter(boolean showInfo, ImageType imageType, int staticHeight) {
        this(showInfo, staticHeight);
        mImageType = imageType;
    }

    public CardPresenter(boolean showInfo, int staticHeight) {
        this(showInfo);
        mStaticHeight = staticHeight;
    }

    class ViewHolder extends Presenter.ViewHolder {
        private int cardWidth = 115;
        private int cardHeight = 140;

        private BaseRowItem mItem;
        private LegacyImageCardView mCardView;
        private Drawable mDefaultCardImage;

        public ViewHolder(View view) {
            super(view);

            mCardView = (LegacyImageCardView) view;
            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
        }

        public int getCardWidth() {
            return cardWidth;
        }

        public int getCardHeight() {
            return cardHeight;
        }

        public void setItem(BaseRowItem m) {
            setItem(m, ImageType.POSTER, 130, 150, 150);
        }

        public void setItem(BaseRowItem m, ImageType imageType, int lHeight, int pHeight, int sHeight) {
            mItem = m;
            isUserView = false;
            switch (mItem.getBaseRowType()) {

                case BaseItem:
                    org.jellyfin.sdk.model.api.BaseItemDto itemDto = mItem.getBaseItem();
                    boolean showWatched = true;
                    boolean showProgress = false;
                    if (imageType.equals(ImageType.BANNER)) {
                        aspect = ImageHelper.ASPECT_RATIO_BANNER;
                    } else if (imageType.equals(ImageType.THUMB)) {
                        aspect = ImageHelper.ASPECT_RATIO_16_9;
                    } else {
                        aspect = imageHelper.getValue().getImageAspectRatio(itemDto, m.getPreferParentThumb());
                    }
                    switch (itemDto.getType()) {
                        case AUDIO:
                        case MUSIC_ALBUM:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_audio);
                            if (isUniformAspect) {
                                aspect = 1.0;
                            } else if (aspect < .8) {
                                aspect = 1.0;
                            }
                            showWatched = false;
                            break;
                        case PERSON:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                            break;
                        case MUSIC_ARTIST:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                            if (isUniformAspect) {
                                aspect = 1.0;
                            } else if (aspect < .8) {
                                aspect = 1.0;
                            }
                            showWatched = false;
                            break;
                        case SEASON:
                        case SERIES:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                            if (imageType.equals(ImageType.POSTER))
                                aspect = ImageHelper.ASPECT_RATIO_2_3;
                            break;
                        case EPISODE:
                            if (m instanceof BaseItemDtoBaseRowItem && ((BaseItemDtoBaseRowItem) m).getPreferSeriesPoster()) {
                                mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                                aspect = ImageHelper.ASPECT_RATIO_2_3;
                            } else {
                                mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_tv);
                                aspect = ImageHelper.ASPECT_RATIO_16_9;
                                if (itemDto.getLocationType() != null) {
                                    switch (itemDto.getLocationType()) {
                                        case FILE_SYSTEM:
                                            break;
                                        case REMOTE:
                                            break;
                                        case VIRTUAL:
                                            mCardView.setBanner(itemDto.getPremiereDate() == null || itemDto.getPremiereDate().isAfter(LocalDateTime.now()) ? R.drawable.banner_edge_future : R.drawable.banner_edge_missing);
                                            break;
                                    }
                                }
                                showProgress = true;
                                //Always show info for episodes
                                mCardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
                            }
                            break;
                        case COLLECTION_FOLDER:
                        case USER_VIEW:
                            // Force the aspect ratio to 16x9 because the server is returning the wrong value of 1
                            // When this is fixed we should still force 16x9 if an image is not set to be consistent
                            aspect = ImageHelper.ASPECT_RATIO_16_9;
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_folder);
                            isUserView = true;
                            break;
                        case FOLDER:
                        case GENRE:
                        case MUSIC_GENRE:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_folder);
                            break;
                        case PHOTO:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_photo);
                            showWatched = false;
                            break;
                        case PHOTO_ALBUM:
                        case PLAYLIST:
                            showWatched = false;
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_folder);
                            break;
                        case MOVIE:
                        case VIDEO:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                            showProgress = true;
                            if (imageType.equals(ImageType.POSTER))
                                aspect = ImageHelper.ASPECT_RATIO_2_3;
                            break;
                        default:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                            if (imageType.equals(ImageType.POSTER))
                                aspect = ImageHelper.ASPECT_RATIO_2_3;
                            break;
                    }
                    cardHeight = !m.getStaticHeight() ? (aspect > 1 ? lHeight : pHeight) : sHeight;
                    cardWidth = (int) (aspect * cardHeight);
                    if (cardWidth < 5) {
                        cardWidth = 115;  //Guard against zero size images causing picasso to barf
                    }
                    if (Utils.isTrue(itemDto.isPlaceHolder())) {
                        mCardView.setBanner(R.drawable.banner_edge_disc);
                    }
                    UserItemDataDto userData = itemDto.getUserData();
                    if (showWatched && userData != null) {
                        WatchedIndicatorBehavior showIndicator = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getWatchedIndicatorBehavior());
                        if (userData.getPlayed()) {
                            if (showIndicator != WatchedIndicatorBehavior.NEVER && (showIndicator != WatchedIndicatorBehavior.EPISODES_ONLY || itemDto.getType() == BaseItemKind.EPISODE))
                                mCardView.setUnwatchedCount(0);
                            else
                                mCardView.setUnwatchedCount(-1);
                        } else if (userData.getUnplayedItemCount() != null) {
                            if (showIndicator == WatchedIndicatorBehavior.ALWAYS)
                                mCardView.setUnwatchedCount(userData.getUnplayedItemCount());
                            else
                                mCardView.setUnwatchedCount(-1);
                        }
                    }

                    if (showProgress && itemDto.getRunTimeTicks() != null && itemDto.getRunTimeTicks() > 0 && userData != null && userData.getPlaybackPositionTicks() > 0) {
                        mCardView.setProgress(((int) (userData.getPlaybackPositionTicks() * 100.0 / itemDto.getRunTimeTicks()))); // force floating pt math with 100.0
                    } else {
                        mCardView.setProgress(0);
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    break;
                case LiveTvChannel:
                    org.jellyfin.sdk.model.api.BaseItemDto channel = mItem.getBaseItem();
                    // TODO: Is it even possible to have channels with banners or thumbs?
                    double tvAspect = imageType.equals(ImageType.BANNER) ? ImageHelper.ASPECT_RATIO_BANNER :
                        imageType.equals(ImageType.THUMB) ? ImageHelper.ASPECT_RATIO_16_9 :
                        Utils.getSafeValue(channel.getPrimaryImageAspectRatio(), 1.0);
                    cardHeight = !m.getStaticHeight() ? tvAspect > 1 ? lHeight : pHeight : sHeight;
                    cardWidth = (int) ((tvAspect) * cardHeight);
                    if (cardWidth < 5) {
                        cardWidth = 115;  //Guard against zero size images causing picasso to barf
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    // Channel logos should fit within the view
                    mCardView.getMainImageView().setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_tv);
                    break;
                case LiveTvProgram:
                    org.jellyfin.sdk.model.api.BaseItemDto program = mItem.getBaseItem();
                    if (program.getLocationType() != null) {
                        switch (program.getLocationType()) {
                            case FILE_SYSTEM:
                            case REMOTE:
                                break;
                            case VIRTUAL:
                                if (program.getStartDate() != null && program.getStartDate().isAfter(LocalDateTime.now())) {
                                    mCardView.setBanner(R.drawable.banner_edge_future);
                                }
                                break;
                        }
                    }
                    mCardView.setMainImageDimensions(192, 129, ImageView.ScaleType.CENTER_INSIDE);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_tv);
                    //Always show info for programs
                    mCardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
                    break;
                case LiveTvRecording:
                    BaseItemDto recording = mItem.getBaseItem();
                    double recordingAspect = imageType.equals(ImageType.BANNER) ? ImageHelper.ASPECT_RATIO_BANNER : (imageType.equals(ImageType.THUMB) ? ImageHelper.ASPECT_RATIO_16_9 : Utils.getSafeValue(recording.getPrimaryImageAspectRatio(), ImageHelper.ASPECT_RATIO_7_9));
                    cardHeight = !m.getStaticHeight() ? recordingAspect > 1 ? lHeight : pHeight : sHeight;
                    cardWidth = (int) ((recordingAspect) * cardHeight);
                    if (cardWidth < 5) {
                        cardWidth = 115;  //Guard against zero size images causing picasso to barf
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                    break;
                case Person:
                    cardHeight = !m.getStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageHelper.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                    break;
                case Chapter:
                    cardHeight = !m.getStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageHelper.ASPECT_RATIO_16_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_chapter);
                    break;
                case GridButton:
                    cardHeight = !m.getStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageHelper.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                    break;
                case SeriesTimer:
                    cardHeight = !m.getStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageHelper.ASPECT_RATIO_16_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_series_timer);
                    //Always show info for timers
                    mCardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
                    break;
            }
        }

        public BaseRowItem getItem() {
            return mItem;
        }

        protected void updateCardViewImage(@Nullable String url, @Nullable String blurHash) {
            mCardView.getMainImageView().load(url, blurHash, mDefaultCardImage, aspect, 32);
        }

        protected void resetCardView() {
            mCardView.clearBanner();
            mCardView.setUnwatchedCount(-1);
            mCardView.setProgress(0);
            mCardView.setRating(null);
            mCardView.setBadgeImage(null);

            mCardView.getMainImageView().setImageDrawable(null);
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
        if (!(item instanceof BaseRowItem)) {
            return;
        }
        BaseRowItem rowItem = (BaseRowItem) item;

        ViewHolder holder = (ViewHolder) viewHolder;
        holder.setItem(rowItem, mImageType, 130, 150, mStaticHeight);

        holder.mCardView.setTitleText(rowItem.getCardName(holder.mCardView.getContext()));
        holder.mCardView.setContentText(rowItem.getSubText(holder.mCardView.getContext()));
        if (ImageType.POSTER.equals(mImageType)) {
            holder.mCardView.setOverlayInfo(rowItem);
        }
        holder.mCardView.showFavIcon(rowItem.isFavorite());
        if (rowItem instanceof AudioQueueBaseRowItem && ((AudioQueueBaseRowItem) rowItem).getPlaying()) {
            holder.mCardView.setPlayingIndicator(true);
        } else {
            holder.mCardView.setPlayingIndicator(false);

            if (rowItem.getBaseItem() != null && rowItem.getBaseItem().getType() != BaseItemKind.USER_VIEW) {
                RatingType ratingType = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDefaultRatingType());
                if (ratingType == RatingType.RATING_TOMATOES) {
                    Drawable badge = rowItem.getBadgeImage(holder.view.getContext(), imageHelper.getValue());
                    holder.mCardView.setRating(null);
                    if (badge != null) {
                        holder.mCardView.setBadgeImage(badge);
                    }
                } else if (ratingType == RatingType.RATING_STARS &&
                        rowItem.getBaseItem().getCommunityRating() != null) {
                    holder.mCardView.setBadgeImage(ContextCompat.getDrawable(viewHolder.view.getContext(), R.drawable.ic_star));
                    holder.mCardView.setRating(String.format(Locale.US, "%.1f", rowItem.getBaseItem().getCommunityRating()));
                }
            }
        }

        JellyfinImage image = null;
        if (rowItem.getBaseItem() != null) {
            if (aspect == ImageHelper.ASPECT_RATIO_BANNER) {
                image = JellyfinImageKt.getItemImages(rowItem.getBaseItem()).get(org.jellyfin.sdk.model.api.ImageType.BANNER);
            } else if (aspect == ImageHelper.ASPECT_RATIO_2_3 && rowItem.getBaseItem().getType() == BaseItemKind.EPISODE && rowItem instanceof BaseItemDtoBaseRowItem && ((BaseItemDtoBaseRowItem) rowItem).getPreferSeriesPoster()) {
                image = JellyfinImageKt.getSeriesPrimaryImage(rowItem.getBaseItem());
            } else if (aspect == ImageHelper.ASPECT_RATIO_16_9 && !isUserView && (rowItem.getBaseItem().getType() != BaseItemKind.EPISODE || !rowItem.getBaseItem().getImageTags().containsKey(org.jellyfin.sdk.model.api.ImageType.PRIMARY) || (rowItem.getPreferParentThumb() && rowItem.getBaseItem().getParentThumbImageTag() != null))) {
                if (rowItem.getPreferParentThumb() || !rowItem.getBaseItem().getImageTags().containsKey(org.jellyfin.sdk.model.api.ImageType.PRIMARY)) {
                    image = JellyfinImageKt.getParentImages(rowItem.getBaseItem()).get(org.jellyfin.sdk.model.api.ImageType.THUMB);
                } else {
                    image = JellyfinImageKt.getItemImages(rowItem.getBaseItem()).get(org.jellyfin.sdk.model.api.ImageType.THUMB);
                }
            } else {
                image = JellyfinImageKt.getItemImages(rowItem.getBaseItem()).get(org.jellyfin.sdk.model.api.ImageType.PRIMARY);
            }
        }

        int fillWidth = Math.round(holder.getCardWidth() * holder.mCardView.getResources().getDisplayMetrics().density);
        int fillHeight = Math.round(holder.getCardHeight() * holder.mCardView.getResources().getDisplayMetrics().density);

        final String imageUrl;
        final String blurHash;
        if (image == null) {
            imageUrl = rowItem.getImageUrl(holder.mCardView.getContext(), imageHelper.getValue(), mImageType, fillWidth, fillHeight);
            blurHash = null;
        } else {
            imageUrl = imageHelper.getValue().getImageUrl(image, fillWidth, fillHeight);
            blurHash = image.getBlurHash();
        }

        holder.updateCardViewImage(imageUrl, blurHash);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((ViewHolder) viewHolder).resetCardView();
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }

    public void setUniformAspect(boolean uniformAspect) {
        isUniformAspect = uniformAspect;
    }
}
