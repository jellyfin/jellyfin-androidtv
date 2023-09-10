package org.jellyfin.androidtv.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.ImageOptions;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.UserDto;
import org.jellyfin.sdk.model.api.UserItemDataDto;
import org.koin.java.KoinJavaComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageUtils {
    public static final double ASPECT_RATIO_2_3 = 2.0 / 3.0;
    public static final double ASPECT_RATIO_16_9 = 16.0 / 9.0;
    public static final double ASPECT_RATIO_7_9 = 7.0 / 9.0;

    public static final int MAX_PRIMARY_IMAGE_HEIGHT = 370;

    private static final List<BaseItemKind> THUMB_FALLBACK_TYPES = Collections.singletonList(BaseItemKind.EPISODE);
    private static final List<BaseItemKind> PROGRESS_INDICATOR_TYPES = Arrays.asList(BaseItemKind.EPISODE, BaseItemKind.MOVIE, BaseItemKind.MUSIC_VIDEO, BaseItemKind.VIDEO);

    public static Double getImageAspectRatio(org.jellyfin.sdk.model.api.BaseItemDto item, boolean preferParentThumb) {
        if (preferParentThumb &&
                (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null)) {
            return ASPECT_RATIO_16_9;
        }

        if (THUMB_FALLBACK_TYPES.contains(item.getType())) {
            if (item.getPrimaryImageAspectRatio() != null) {
                return item.getPrimaryImageAspectRatio();
            }
            if (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null) {
                return ASPECT_RATIO_16_9;
            }
        }

        if (item.getType() == BaseItemKind.USER_VIEW && item.getImageTags().containsKey(org.jellyfin.sdk.model.api.ImageType.PRIMARY))
            return ImageUtils.ASPECT_RATIO_16_9;

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : ASPECT_RATIO_7_9;
    }

    public static String getPrimaryImageUrl(@NonNull BaseItemPerson item, @Nullable int maxHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, maxHeight);
    }

    public static String getPrimaryImageUrl(@NonNull UserDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item);
    }

    public static String getPrimaryImageUrl(@NonNull org.jellyfin.sdk.model.api.BaseItemDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, null, MAX_PRIMARY_IMAGE_HEIGHT);
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

    public static String getBannerImageUrl(org.jellyfin.sdk.model.api.BaseItemDto item, ApiClient apiClient, int fillWidth, int fillHeight) {
        if (!item.getImageTags().containsKey(org.jellyfin.sdk.model.api.ImageType.BANNER)) {
            return getPrimaryImageUrl(item, false, fillWidth, fillHeight);
        }

        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Banner));
        options.setImageType(ImageType.Banner);

        UserItemDataDto userData = item.getUserData();
        if (userData != null && item.getType() != BaseItemKind.MUSIC_ARTIST && item.getType() != BaseItemKind.MUSIC_ALBUM) {
            if (PROGRESS_INDICATOR_TYPES.contains(item.getType()) &&
                    userData.getPlayedPercentage() != null &&
                    userData.getPlayedPercentage() > 0 &&
                    userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
        }

        return apiClient.GetImageUrl(item.getId().toString(), options);
    }

    public static String getThumbImageUrl(BaseItemDto item, ApiClient apiClient, int fillWidth, int fillHeight) {
        if (!item.getImageTags().containsKey(org.jellyfin.sdk.model.api.ImageType.THUMB)) {
            return getPrimaryImageUrl(item, true, fillWidth, fillHeight);
        }

        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Thumb));
        options.setImageType(ImageType.Thumb);
        return apiClient.GetImageUrl(item.getId().toString(), options);
    }

    public static String getPrimaryImageUrl(@NonNull org.jellyfin.sdk.model.api.BaseItemDto item, boolean preferParentThumb, @Nullable Integer fillWidth, @Nullable Integer fillHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, preferParentThumb, fillWidth, fillHeight);
    }

    public static String getLogoImageUrl(@Nullable org.jellyfin.sdk.model.api.BaseItemDto item, int maxWidth, boolean useSeriesFallback) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getLogoImageUrl(item, maxWidth, useSeriesFallback);
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
