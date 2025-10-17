package org.jellyfin.androidtv.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.albumPrimaryImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.androidtv.util.apiclient.seriesPrimaryImage
import org.jellyfin.androidtv.util.apiclient.seriesThumbImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserDto

class ImageHelper(
	private val api: ApiClient,
) {
	companion object {
		const val ASPECT_RATIO_2_3 = 2.0 / 3.0
		const val ASPECT_RATIO_16_9 = 16.0 / 9.0
		const val ASPECT_RATIO_7_9 = 7.0 / 9.0
		const val ASPECT_RATIO_BANNER = 1000.0 / 185.0

		const val MAX_PRIMARY_IMAGE_HEIGHT: Int = 370
	}

	fun getImageUrl(image: JellyfinImage, fillWidth: Int, fillHeight: Int): String =
		image.getUrl(api, null, null, fillWidth, fillHeight)

	fun getImageAspectRatio(item: BaseItemDto, preferParentThumb: Boolean): Double {
		if (preferParentThumb && (item.parentThumbItemId != null || item.seriesThumbImageTag != null)) {
			return ASPECT_RATIO_16_9
		}

		val primaryAspectRatio = item.primaryImageAspectRatio;
		if (item.type == BaseItemKind.EPISODE) {
			if (primaryAspectRatio != null) return primaryAspectRatio
			if (item.parentThumbItemId != null || item.seriesThumbImageTag != null) return ASPECT_RATIO_16_9
		}

		if (item.type == BaseItemKind.USER_VIEW && item.imageTags?.containsKey(ImageType.PRIMARY) == true) return ASPECT_RATIO_16_9
		return primaryAspectRatio ?: ASPECT_RATIO_7_9
	}

	fun getPrimaryImageUrl(
		item: BaseItemPerson,
		maxHeight: Int? = null,
	): String? = item.primaryImage?.getUrl(api, maxHeight = maxHeight)

	fun getPrimaryImageUrl(
		item: UserDto,
	): String? = item.primaryImage?.getUrl(api)

	fun getPrimaryImageUrl(
		item: BaseItemDto,
		width: Int? = null,
		height: Int? = null,
	): String? = item.itemImages[ImageType.PRIMARY]?.getUrl(api, maxWidth = width, maxHeight = height)

	fun getPrimaryImageUrl(
		item: BaseItemDto,
		preferParentThumb: Boolean,
		fillWidth: Int? = null,
		fillHeight: Int? = null
	): String? {
		val image = when {
			preferParentThumb && item.type == BaseItemKind.EPISODE -> item.parentImages[ImageType.THUMB] ?: item.seriesThumbImage
			item.type == BaseItemKind.SEASON -> item.seriesPrimaryImage
			item.type == BaseItemKind.PROGRAM && item.imageTags?.containsKey(ImageType.THUMB) == true -> item.itemImages[ImageType.THUMB]
			item.type == BaseItemKind.AUDIO -> item.albumPrimaryImage
			else -> null
		} ?: item.itemImages[ImageType.PRIMARY]

		return image?.getUrl(
			api = api,
			fillWidth = fillWidth,
			fillHeight = fillHeight,
		)
	}

	fun getLogoImageUrl(
		item: BaseItemDto?,
		maxWidth: Int? = null
	): String? {
		val image = item?.itemImages[ImageType.LOGO] ?: item?.parentImages[ImageType.LOGO]
		return image?.getUrl(api, maxWidth = maxWidth)
	}

	fun getThumbImageUrl(
		item: BaseItemDto,
		fillWidth: Int,
		fillHeight: Int,
	): String? = item.itemImages[ImageType.THUMB]?.getUrl(api, fillWidth = fillWidth, fillHeight = fillHeight)
		?: getPrimaryImageUrl(item, true, fillWidth, fillHeight)

	fun getBannerImageUrl(item: BaseItemDto, fillWidth: Int, fillHeight: Int): String? =
		item.itemImages[ImageType.BANNER]?.getUrl(api, fillWidth = fillWidth, fillHeight = fillHeight)
			?: getPrimaryImageUrl(item, true, fillWidth, fillHeight)

	/**
	 * A utility to return a URL reference to an image resource
	 *
	 * @param resourceId The id of the image resource
	 * @return The URL of the image resource
	 */
	fun getResourceUrl(
		context: Context,
		@AnyRes resourceId: Int,
	): String = Uri.Builder()
		.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
		.authority(context.resources.getResourcePackageName(resourceId))
		.appendPath(context.resources.getResourceTypeName(resourceId))
		.appendPath(context.resources.getResourceEntryName(resourceId))
		.toString()
}
