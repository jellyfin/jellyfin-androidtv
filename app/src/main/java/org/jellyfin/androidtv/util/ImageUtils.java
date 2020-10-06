package org.jellyfin.androidtv.util;

import android.content.ContentResolver;
import android.content.res.Resources;

import androidx.annotation.AnyRes;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.ImageOptions;
import org.jellyfin.apiclient.model.dto.StudioDto;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.dto.UserItemDataDto;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.koin.java.KoinJavaComponent.get;

public class ImageUtils {
    public static final double ASPECT_RATIO_2_3 = .66667;
    public static final double ASPECT_RATIO_16_9 = 1.779;
    public static final double ASPECT_RATIO_7_9 = .777777777;

    private static final int MAX_PRIMARY_IMAGE_HEIGHT = 370;

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

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : ASPECT_RATIO_7_9;
    }

    public static String getPrimaryImageUrl(BaseItemPerson item, ApiClient apiClient, int maxHeight) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(maxHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetPersonImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(StudioDto studio, ApiClient apiClient, int maxHeight) {
        ImageOptions options = new ImageOptions();
        options.setTag(studio.getPrimaryImageTag());
        options.setMaxHeight(maxHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(studio.getId(), options);
    }

    public static String getPrimaryImageUrl(UserDto item, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(MAX_PRIMARY_IMAGE_HEIGHT);
        options.setImageType(ImageType.Primary);
        return apiClient.GetUserImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, int width, int height) {
        if (!item.getHasPrimaryImage()) {
            return null;
        }
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxWidth(width);
        options.setMaxHeight(height);
        options.setImageType(ImageType.Primary);
        return get(ApiClient.class).GetImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient) {
        if (!item.getHasPrimaryImage()) {
            return null;
        }
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxHeight(MAX_PRIMARY_IMAGE_HEIGHT);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(item, options);
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

    public static String getImageUrl(String itemId, ImageType imageType, String imageTag, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setMaxHeight(MAX_PRIMARY_IMAGE_HEIGHT);
        options.setImageType(imageType);
        options.setTag(imageTag);
        return apiClient.GetImageUrl(itemId, options);
    }

    public static String getBannerImageUrl(BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasBanner()) {
            return getPrimaryImageUrl(item, apiClient, false, maxHeight);
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

    public static String getThumbImageUrl(BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasThumb()) {
            return getPrimaryImageUrl(item, apiClient, true, maxHeight);
        }

        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Thumb));
        options.setImageType(ImageType.Thumb);
        return apiClient.GetImageUrl(item.getId(), options);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, boolean preferParentThumb, int maxHeight) {
        return getPrimaryImageUrl(item, apiClient, false, preferParentThumb, false, maxHeight);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, boolean showProgress, boolean preferParentThumb, boolean preferSeriesPoster, int maxHeight) {
        if (item.getBaseItemType() == BaseItemType.SeriesTimer) {
            return getResourceUrl(R.drawable.tile_land_series_timer);
        }

        ImageOptions options = new ImageOptions();
        String itemId = item.getId();
        String imageTag = item.getImageTags() != null ? item.getImageTags().get(ImageType.Primary) : null;
        ImageType imageType = ImageType.Primary;

        if (preferSeriesPoster && item.getBaseItemType() == BaseItemType.Episode) {
            if (item.getSeasonId() != null) {
                imageTag = null;
                itemId = item.getSeasonId();
            } else if (item.getSeriesPrimaryImageTag() != null && item.getSeriesId() != null) {
                imageTag = item.getSeriesPrimaryImageTag();
                itemId = item.getSeriesId();
            }
        } else if (preferParentThumb || (item.getBaseItemType() == BaseItemType.Episode && imageTag == null)) {
            //try for thumb of season or series
            if (item.getParentThumbImageTag() != null) {
                imageTag = item.getParentThumbImageTag();
                itemId = item.getParentThumbItemId();
                imageType = ImageType.Thumb;
            } else if (item.getSeriesThumbImageTag() != null) {
                imageTag = item.getSeriesThumbImageTag();
                itemId = item.getSeriesId();
                imageType = ImageType.Thumb;
            }
        } else {
            if (item.getBaseItemType() == BaseItemType.Season && imageTag == null) {
                imageTag = item.getSeriesPrimaryImageTag();
                itemId = item.getSeriesId();
            } else if (item.getBaseItemType() == BaseItemType.Program && item.getHasThumb()) {
                imageTag = item.getImageTags().get(ImageType.Thumb);
                imageType = ImageType.Thumb;
            }
        }

        if (item.getBaseItemType() == BaseItemType.Audio && !item.getHasPrimaryImage()) {
            //Try the album or artist
            if (item.getAlbumId() != null && item.getAlbumPrimaryImageTag() != null) {
                imageTag = item.getAlbumPrimaryImageTag();
                itemId = item.getAlbumId();
                imageType = ImageType.Primary;
            } else if (item.getAlbumArtists() != null && item.getAlbumArtists().size() > 0) {
                itemId = item.getAlbumArtists().get(0).getId();
                imageTag = null;
            }
        }
        options.setMaxHeight(maxHeight);
        options.setImageType(imageType);
        options.setTag(imageTag);

        UserItemDataDto userData = item.getUserData();
        if (userData != null) {
            if (showProgress && PROGRESS_INDICATOR_TYPES.contains(item.getBaseItemType()) &&
                    userData.getPlayedPercentage() != null &&
                    userData.getPlayedPercentage() > 0 &&
                    userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
        }

        return apiClient.GetImageUrl(itemId, options);
    }

    public static String getLogoImageUrl(BaseItemDto item, ApiClient apiClient) {
        return getLogoImageUrl(item, apiClient, 440);
    }

    public static String getLogoImageUrl(BaseItemDto item, ApiClient apiClient, int maxWidth) {
        if (item != null) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(maxWidth);
            options.setImageType(ImageType.Logo);
            if (item.getHasLogo()) {
                options.setTag(item.getImageTags().get(ImageType.Logo));
                return apiClient.GetImageUrl(item, options);
            } else if (item.getParentLogoImageTag() != null) {
                options.setTag(item.getParentLogoImageTag());
                return apiClient.GetImageUrl(item.getParentLogoItemId(), options);
            } else if (item.getSeriesId() != null) {
                options.setTag(null);
                return apiClient.GetImageUrl(item.getSeriesId(), options);
            }
        }

        return null;
    }

    public static String getBackdropImageUrl(BaseItemDto item, ApiClient apiClient, boolean random) {
        if (item != null) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(1200);
            options.setImageType(ImageType.Backdrop);

            if (item.getBackdropCount() > 0) {
                int index = random ? MathUtils.randInt(0, item.getBackdropCount() - 1) : 0;
                options.setImageIndex(index);
                options.setTag(item.getBackdropImageTags().get(index));
                return apiClient.GetImageUrl(item, options);
            } else if (item.getParentBackdropImageTags() != null && item.getParentBackdropImageTags().size() > 0) {
                int index = random ? MathUtils.randInt(0, item.getParentBackdropImageTags().size() - 1) : 0;
                options.setImageIndex(index);
                options.setTag(item.getParentBackdropImageTags().get(index));
                return apiClient.GetImageUrl(item.getParentBackdropItemId(), options);
            }
        }

        return null;
    }

    /**
     * A utility to return a URL reference to an image resource
     *
     * @param resourceId The id of the image resource
     * @return The URL of the image resource
     */
    public static String getResourceUrl(@AnyRes int resourceId) {
        Resources resources = TvApp.getApplication().getApplicationContext().getResources();

        return String.format("%s://%s/%s/%s", ContentResolver.SCHEME_ANDROID_RESOURCE, resources.getResourcePackageName(resourceId), resources.getResourceTypeName(resourceId), resources.getResourceEntryName(resourceId));
    }
}
