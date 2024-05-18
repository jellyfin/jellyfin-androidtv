package org.jellyfin.androidtv.util;

import android.content.Context;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jellyfin.androidtv.util.sdk.compat.ModelCompat;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemPerson;
import org.jellyfin.sdk.model.api.UserDto;
import org.koin.java.KoinJavaComponent;

import java.util.UUID;

public class ImageUtils {
    public static Double getImageAspectRatio(org.jellyfin.sdk.model.api.BaseItemDto item, boolean preferParentThumb) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getImageAspectRatio(item, preferParentThumb);
    }

    public static String getPrimaryImageUrl(@NonNull BaseItemPerson item, @Nullable int maxHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, maxHeight);
    }

    public static String getPrimaryImageUrl(@NonNull UserDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item);
    }

    public static String getPrimaryImageUrl(@NonNull org.jellyfin.sdk.model.api.BaseItemDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT);
    }

    public static String getPrimaryImageUrl(ChannelInfoDto item) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(ModelCompat.asSdk(item), null, ImageHelper.MAX_PRIMARY_IMAGE_HEIGHT);
    }

    public static String getImageUrl(@NonNull UUID itemId, @NonNull ImageType imageType, @NonNull String imageTag) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getImageUrl(itemId, ModelCompat.asSdk(imageType), imageTag);
    }

    public static String getBannerImageUrl(org.jellyfin.sdk.model.api.BaseItemDto item, int fillWidth, int fillHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getBannerImageUrl(item, fillWidth, fillHeight);
    }

    public static String getThumbImageUrl(BaseItemDto item, int fillWidth, int fillHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getThumbImageUrl(item, fillWidth, fillHeight);
    }

    public static String getPrimaryImageUrl(@NonNull org.jellyfin.sdk.model.api.BaseItemDto item, boolean preferParentThumb, @Nullable Integer fillWidth, @Nullable Integer fillHeight) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getPrimaryImageUrl(item, preferParentThumb, fillWidth, fillHeight);
    }

    public static String getLogoImageUrl(@Nullable org.jellyfin.sdk.model.api.BaseItemDto item, int maxWidth, boolean useSeriesFallback) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getLogoImageUrl(item, maxWidth, useSeriesFallback);
    }

    public static String getResourceUrl(Context context, @AnyRes int resourceId) {
        return KoinJavaComponent.<ImageHelper>get(ImageHelper.class).getResourceUrl(context, resourceId);
    }
}
