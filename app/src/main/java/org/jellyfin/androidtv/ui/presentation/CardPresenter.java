package org.jellyfin.androidtv.ui.presentation;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.RatingType;
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

import java.util.Date;

import timber.log.Timber;

import static org.koin.java.KoinJavaComponent.get;

public class CardPresenter extends Presenter {
    private static final double ASPECT_RATIO_BANNER = 5.414;

    private int mStaticHeight = 300;
    private String mImageType = ImageType.DEFAULT;

    private boolean mShowInfo = true;

    public CardPresenter() {
        super();
    }

    public CardPresenter(boolean showInfo) {
        this();
        mShowInfo = showInfo;
    }

    public CardPresenter(boolean showInfo, String imageType, int staticHeight) {
        this(showInfo, staticHeight);
        mImageType = imageType;
    }

    public CardPresenter(boolean showInfo, int staticHeight) {
        this(showInfo);
        mStaticHeight = staticHeight;
    }

    static class ViewHolder extends Presenter.ViewHolder {
        private int cardWidth = 230;
        private int cardHeight = 280;

        private BaseRowItem mItem;
        private MyImageCardView mCardView;
        private Drawable mDefaultCardImage;

        public ViewHolder(View view) {
            super(view);

            mCardView = (MyImageCardView) view;
            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
        }

        public int getCardHeight() {
            return cardHeight;
        }

        public void setItem(BaseRowItem m) {
            setItem(m, ImageType.DEFAULT, 260, 300, 300);
        }

        public void setItem(BaseRowItem m, String imageType, int lHeight, int pHeight, int sHeight) {
            mItem = m;
            switch (mItem.getItemType()) {

                case BaseItem:
                    BaseItemDto itemDto = mItem.getBaseItem();
                    boolean showWatched = true;
                    boolean showProgress = false;
                    double aspect;
                    if (imageType.equals(ImageType.BANNER)) {
                        aspect = ASPECT_RATIO_BANNER;
                    } else if (imageType.equals(ImageType.THUMB)) {
                        aspect = ImageUtils.ASPECT_RATIO_16_9;
                    } else {
                        aspect = Utils.getSafeValue(ImageUtils.getImageAspectRatio(itemDto, m.getPreferParentThumb()), ImageUtils.ASPECT_RATIO_7_9);
                    }
                    switch (itemDto.getBaseItemType()) {
                        case Audio:
                        case MusicAlbum:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_audio);
                            if (aspect < 0.8) {
                                aspect = 1.0;
                            }
                            showWatched = false;
                            break;
                        case Person:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                            break;
                        case MusicArtist:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                            if (aspect < .8) {
                                aspect = 1.0;
                            }
                            showWatched = false;
                            break;
                        case RecordingGroup:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_record);
                            break;
                        case Season:
                        case Series:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                            break;
                        case Episode:
                            //TvApp.getApplication().getLogger().Debug("**** Image width: "+ cardWidth + " Aspect: " + Utils.getImageAspectRatio(itemDto, m.getPreferParentThumb()) + " Item: "+itemDto.getName());
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_tv);
                            switch (itemDto.getLocationType()) {
                                case FileSystem:
                                    break;
                                case Remote:
                                    break;
                                case Virtual:
                                    mCardView.setBanner((itemDto.getPremiereDate() != null ? TimeUtils.convertToLocalDate(itemDto.getPremiereDate()) : new Date(System.currentTimeMillis() + 1)).getTime() > System.currentTimeMillis() ? R.drawable.banner_edge_future : R.drawable.banner_edge_missing);
                                    break;
                                case Offline:
                                    mCardView.setBanner(R.drawable.banner_edge_offline);
                                    break;
                            }
                            showProgress = true;
                            //Always show info for episodes
                            mCardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
                            break;
                        case CollectionFolder:
                        case UserView:
                            // Force the aspect ratio to 16x9 because the server is returning the wrong value of 1
                            // When this is fixed we should still force 16x9 if an image is not set to be consistent
                            aspect = ImageUtils.ASPECT_RATIO_16_9;
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_folder);
                            break;
                        case Folder:
                        case MovieGenreFolder:
                        case MusicGenreFolder:
                        case MovieGenre:
                        case Genre:
                        case MusicGenre:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_folder);
                            break;
                        case Photo:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_photo);
                            showWatched = false;
                            break;
                        case PhotoAlbum:
                        case Playlist:
                            showWatched = false;
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_folder);
                            break;
                        case Movie:
                        case Video:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                            showProgress = true;
                            break;
                        default:
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                            break;
                    }
                    cardHeight = !m.isStaticHeight() ? (aspect > 1 ? lHeight : pHeight) : sHeight;
                    cardWidth = (int) (aspect * cardHeight);
                    if (cardWidth < 10) {
                        cardWidth = 230;  //Guard against zero size images causing picasso to barf
                    }
                    if (itemDto.getLocationType() == LocationType.Offline) {
                        mCardView.setBanner(R.drawable.banner_edge_offline);
                    }
                    if (itemDto.getIsPlaceHolder() != null && itemDto.getIsPlaceHolder()) {
                        mCardView.setBanner(R.drawable.banner_edge_disc);
                    }
                    UserItemDataDto userData = itemDto.getUserData();
                    if (showWatched && userData != null) {
                        WatchedIndicatorBehavior showIndicator = get(UserPreferences.class).get(UserPreferences.Companion.getWatchedIndicatorBehavior());
                        if (userData.getPlayed()) {
                            if (showIndicator != WatchedIndicatorBehavior.NEVER && (showIndicator != WatchedIndicatorBehavior.EPISODES_ONLY || itemDto.getBaseItemType() == BaseItemType.Episode))
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
                    ChannelInfoDto channel = mItem.getChannelInfo();
                    double tvAspect = imageType.equals(ImageType.BANNER) ? ASPECT_RATIO_BANNER : imageType.equals(ImageType.THUMB) ? ImageUtils.ASPECT_RATIO_16_9 : Utils.getSafeValue(channel.getPrimaryImageAspectRatio(), ImageUtils.ASPECT_RATIO_7_9);
                    cardHeight = !m.isStaticHeight() ? tvAspect > 1 ? lHeight : pHeight : sHeight;
                    cardWidth = (int) ((tvAspect) * cardHeight);
                    if (cardWidth < 10) {
                        cardWidth = 230;  //Guard against zero size images causing picasso to barf
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                    break;
                case LiveTvProgram:
                    BaseItemDto program = mItem.getProgramInfo();
                    Double programAspect = program.getPrimaryImageAspectRatio();
                    if (Utils.isTrue(program.getIsMovie())) {
                        // The server reports the incorrect image aspect ratio for movies, so we are overriding it here
                        programAspect = ImageUtils.ASPECT_RATIO_2_3;
                    } else if (programAspect == null) {
                        programAspect = ImageUtils.ASPECT_RATIO_16_9;
                    }
                    cardHeight = !m.isStaticHeight() ? programAspect > 1 ? lHeight : pHeight : sHeight;
                    cardWidth = (int) ((programAspect) * cardHeight);
                    if (cardWidth < 10) {
                        cardWidth = 230;  //Guard against zero size images causing picasso to barf
                    }
                    switch (program.getLocationType()) {
                        case FileSystem:
                        case Remote:
                        case Offline:
                            break;
                        case Virtual:
                            if (program.getStartDate() != null && TimeUtils.convertToLocalDate(program.getStartDate()).getTime() > System.currentTimeMillis()) {
                                mCardView.setBanner(R.drawable.banner_edge_future);
                            }
                            break;
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_land_tv);
                    //Always show info for programs
                    mCardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
                    break;
                case LiveTvRecording:
                    BaseItemDto recording = mItem.getRecordingInfo();
                    double recordingAspect = imageType.equals(ImageType.BANNER) ? ASPECT_RATIO_BANNER : (imageType.equals(ImageType.THUMB) ? ImageUtils.ASPECT_RATIO_16_9 : Utils.getSafeValue(recording.getPrimaryImageAspectRatio(), ImageUtils.ASPECT_RATIO_7_9));
                    cardHeight = !m.isStaticHeight() ? recordingAspect > 1 ? lHeight : pHeight : sHeight;
                    cardWidth = (int) ((recordingAspect) * cardHeight);
                    if (cardWidth < 10) {
                        cardWidth = 230;  //Guard against zero size images causing picasso to barf
                    }
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                    break;
                case Server:
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_server);
                    break;
                case Person:
                    cardHeight = !m.isStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                    break;
                case User:
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                    break;
                case Chapter:
                    cardHeight = !m.isStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_16_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_chapter);
                    break;
                case SearchHint:
                    switch (mItem.getSearchHint().getType()) {
                        case "Episode":
                            cardWidth = (int) (ImageUtils.ASPECT_RATIO_16_9 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_tv);
                            break;
                        case "Person":
                            cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_person);
                            break;
                        default:
                            cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                            mCardView.setMainImageDimensions(cardWidth, cardHeight);
                            mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                            break;
                    }
                    break;
                case GridButton:
                    cardHeight = !m.isStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_7_9 * cardHeight);
                    mCardView.setMainImageDimensions(cardWidth, cardHeight);
                    mDefaultCardImage = ContextCompat.getDrawable(mCardView.getContext(), R.drawable.tile_port_video);
                    break;
                case SeriesTimer:
                    cardHeight = !m.isStaticHeight() ? pHeight : sHeight;
                    cardWidth = (int) (ImageUtils.ASPECT_RATIO_16_9 * cardHeight);
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

        protected void updateCardViewImage(@Nullable String url) {
            try {
                if (url == null) {
                    Glide.with(mCardView.getContext())
                            .load(mDefaultCardImage)
                            .into(mCardView.getMainImageView());
                } else {
                    Glide.with(mCardView.getContext())
                            .load(url)
                            .error(mDefaultCardImage)
                            .into(mCardView.getMainImageView());
                }
            } catch (IllegalArgumentException e) {
                Timber.i("Image load aborted due to activity closing");
            }
        }

        protected void resetCardView() {
            mCardView.clearBanner();
            mCardView.setUnwatchedCount(-1);
            mCardView.setProgress(0);
            mCardView.setRating(null);
            mCardView.setBadgeImage(null);

            mCardView.getMainImageView().setImageResource(R.drawable.loading);
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
        if (!(item instanceof BaseRowItem)) {
            return;
        }
        BaseRowItem rowItem = (BaseRowItem) item;
        if (!rowItem.isValid()) {
            return;
        }

        ViewHolder holder = (ViewHolder) viewHolder;
        holder.setItem(rowItem, mImageType, 260, 300, mStaticHeight);

        holder.mCardView.setTitleText(rowItem.getCardName());
        holder.mCardView.setContentText(rowItem.getSubText(holder.mCardView.getContext()));
        if (ImageType.DEFAULT.equals(mImageType)) {
            holder.mCardView.setOverlayInfo(rowItem);
        }
        holder.mCardView.showFavIcon(rowItem.isFavorite());
        if (rowItem.isPlaying()) {
            holder.mCardView.setPlayingIndicator(true);
        } else {
            holder.mCardView.setPlayingIndicator(false);

            if (holder.getItem().getBaseItemType() != BaseItemType.UserView) {
                RatingType ratingType = get(UserPreferences.class).get(UserPreferences.Companion.getDefaultRatingType());
                if (ratingType == RatingType.RATING_TOMATOES) {
                    Drawable badge = rowItem.getBadgeImage();
                    holder.mCardView.setRating(null);
                    if (badge != null) {
                        holder.mCardView.setBadgeImage(badge);
                    }
                } else if (ratingType == RatingType.RATING_STARS &&
                        rowItem.getBaseItem() != null && rowItem.getBaseItem().getCommunityRating() != null) {
                    holder.mCardView.setBadgeImage(ContextCompat.getDrawable(viewHolder.view.getContext(), R.drawable.ic_star));
                    holder.mCardView.setRating(rowItem.getBaseItem().getCommunityRating().toString());
                }
            }
        }

        holder.updateCardViewImage(rowItem.getImageUrl(holder.mCardView.getContext(), mImageType, holder.getCardHeight()));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((ViewHolder) viewHolder).resetCardView();
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
    }
}
