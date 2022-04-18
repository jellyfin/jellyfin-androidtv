package org.jellyfin.androidtv.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.ImageOptions;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.sdk.model.api.UserDto;
import org.koin.java.KoinJavaComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageUtils {
    public static final double ASPECT_RATIO_2_3 = 2.0 / 3.0;
    public static final double ASPECT_RATIO_16_9 = 16.0 / 9.0;
    public static final double ASPECT_RATIO_7_9 = 7.0 / 9.0;

    public static final int MAX_PRIMARY_IMAGE_HEIGHT = 370;

    private static final List<BaseItemType> THUMB_FALLBACK_TYPES = Collections.singletonList(BaseItemType.Episode);
    private static final List<BaseItemType> PROGRESS_INDICATOR_TYPES = Arrays.asList(BaseItemType.Episode, BaseItemType.Movie, BaseItemType.MusicVideo, BaseItemType.Video);

    public static Double getImageAspectRatio(BaseItemDto item, boolean preferParentThumb) {
        if (preferParentThumb &&
                (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null)) {
            return ASPECT_RATIO_16_9;
        }

        if (THUMB_FALLBACK_TYPES.contains(item.getBaseItemType())) {
            if (item.getPrimaryImageAspectRatio() != null) {
                return item.getPrimaryImageAspectRatio();
            }
            if (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null) {
                return ASPECT_RATIO_16_9;
            }
        }

        if (item.getBaseItemType() == BaseItemType.UserView && item.getHasPrimaryImage())
            return ImageUtils.ASPECT_RATIO_16_9;

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : ASPECT_RATIO_7_9;
    }

    public static String getPrimaryImageUrl(@NonNull BaseItemPerson item, @Nullable int maxHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(ModelCompat.asSdk(item), maxHeight);
    }

    public static String getPrimaryImageUrl(@NonNull UserDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item);
    }

    public static String getPrimaryImageUrl(@NonNull BaseItemDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(ModelCompat.asSdk(item), null, MAX_PRIMARY_IMAGE_HEIGHT);
    }

    public static String getPrimaryImageUrl(ChannelInfoDto item, ApiClient apiClient) {
        if (!item.getHasPrimaryImage()) {
            return null;
        }
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxHeight(MAX_PRIMARY_IMAGE_HEIGHT);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(item, options);
    }

    public static String getImageUrl(@NonNull String itemId, @NonNull ImageType imageType, @NonNull String imageTag) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getImageUrl(itemId, ModelCompat.asSdk(imageType), imageTag);
    }

    public static String getBannerImageUrl(Context context, BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasBanner()) {
            return getPrimaryImageUrl(context, item, false, maxHeight);
        }

        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Banner));
        options.setImageType(ImageType.Banner);

        UserItemDataDto userData = item.getUserData();
        if (userData != null && item.getBaseItemType() != BaseItemType.MusicArtist && item.getBaseItemType() != BaseItemType.MusicAlbum) {
            if (PROGRESS_INDICATOR_TYPES.contains(item.getBaseItemType()) &&
                    userData.getPlayedPercentage() != null &&
                    userData.getPlayedPercentage() > 0 &&
                    userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
        }

        return apiClient.GetImageUrl(item.getId(), options);
    }

    public static String getThumbImageUrl(Context context, BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasThumb()) {
            return getPrimaryImageUrl(context, item, true, maxHeight);
        }

        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Thumb));
        options.setImageType(ImageType.Thumb);
        return apiClient.GetImageUrl(item.getId(), options);
    }

    public static String getPrimaryImageUrl(@NonNull Context context, @NonNull BaseItemDto item, @NonNull boolean preferParentThumb, @NonNull int maxHeight) {
        if (item.getBaseItemType() == BaseItemType.SeriesTimer) {
            return getResourceUrl(context, R.drawable.tile_land_series_timer);
        }

        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(ModelCompat.asSdk(item), preferParentThumb, maxHeight);
    }

    public static String getLogoImageUrl(@Nullable BaseItemDto item, @NonNull int maxWidth, @NonNull boolean useSeriesFallback) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getLogoImageUrl(ModelCompat.asSdk(item), maxWidth, useSeriesFallback);
    }

    /**
     * A utility to return a URL reference to an image resource
     *
     * @param resourceId The id of the image resource
     * @return The URL of the image resource
     */
    public static String getResourceUrl(Context context, @AnyRes int resourceId) {
        Resources resources = context.getResources();

        return String.format("%s://%s/%s/%s", ContentResolver.SCHEME_ANDROID_RESOURCE, resources.getResourcePackageName(resourceId), resources.getResourceTypeName(resourceId), resources.getResourceEntryName(resourceId));
    }
}
