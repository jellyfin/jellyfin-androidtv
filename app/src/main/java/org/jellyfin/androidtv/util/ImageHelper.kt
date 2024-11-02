package org.jellyfin.androidtv.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.UserDto
import java.util.UUID

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

	fun getPrimaryImageUrl(item: BaseItemPerson, maxHeight: Int? = null): String? {
		if (item.primaryImageTag == null) return null

		return item.id.let { itemId ->
			api.imageApi.getItemImageUrl(
				itemId = itemId,
				imageType = ImageType.PRIMARY,
				tag = item.primaryImageTag,
				maxHeight = maxHeight,
			)
		}
	}

	fun getPrimaryImageUrl(item: UserDto): String? {
		if (item.primaryImageTag == null) return null

		return api.imageApi.getUserImageUrl(
			userId = item.id,
			tag = item.primaryImageTag,
			maxHeight = MAX_PRIMARY_IMAGE_HEIGHT,
		)
	}

	fun getPrimaryImageUrl(item: BaseItemDto, width: Int? = null, height: Int? = null): String? {
		val primaryImageTag = item.imageTags?.get(ImageType.PRIMARY) ?: return null

		return api.imageApi.getItemImageUrl(
			itemId = item.id,
			imageType = ImageType.PRIMARY,
			tag = primaryImageTag,
			maxWidth = width,
			maxHeight = height,
		)
	}

	fun getImageUrl(itemId: UUID, imageType: ImageType, imageTag: String): String =
		api.imageApi.getItemImageUrl(
			itemId = itemId,
			imageType = imageType,
			tag = imageTag,
			maxHeight = MAX_PRIMARY_IMAGE_HEIGHT,
		)

	fun getPrimaryImageUrl(
		item: BaseItemDto,
		preferParentThumb: Boolean,
		fillWidth: Int? = null,
		fillHeight: Int? = null
	): String {
		var itemId = item.id
		var imageTag = item.imageTags?.get(ImageType.PRIMARY)
		var imageType = ImageType.PRIMARY

		if (preferParentThumb && item.type == BaseItemKind.EPISODE) {
			if (item.parentThumbItemId != null && item.parentThumbImageTag != null) {
				itemId = item.parentThumbItemId!!
				imageTag = item.parentThumbImageTag
				imageType = ImageType.THUMB
			} else if (item.seriesId != null && item.seriesThumbImageTag != null) {
				itemId = item.seriesId!!
				imageTag = item.seriesThumbImageTag
				imageType = ImageType.THUMB
			}
		} else if (item.type == BaseItemKind.SEASON && imageTag == null) {
			if (item.seriesId != null && item.seriesPrimaryImageTag != null) {
				itemId = item.seriesId!!
				imageTag = item.seriesPrimaryImageTag
			}
		} else if (item.type == BaseItemKind.PROGRAM && item.imageTags?.containsKey(ImageType.THUMB) == true) {
			imageTag = item.imageTags!![ImageType.THUMB]
			imageType = ImageType.THUMB
		} else if (item.type == BaseItemKind.AUDIO && imageTag == null) {
			if (item.albumId != null && item.albumPrimaryImageTag != null) {
				itemId = item.albumId!!
				imageTag = item.albumPrimaryImageTag
			} else if (!item.artistItems.isNullOrEmpty()) {
				itemId = item.artistItems!!.first().id
				imageTag = null
			} else if (!item.albumArtists.isNullOrEmpty()) {
				itemId = item.albumArtists!!.first().id
				imageTag = null
			}
		}

		return api.imageApi.getItemImageUrl(
			itemId = itemId,
			imageType = imageType,
			tag = imageTag,
			fillWidth = fillWidth,
			fillHeight = fillHeight,
		)
	}

	fun getLogoImageUrl(
		item: BaseItemDto?,
		maxWidth: Int? = null,
		useSeriesFallback: Boolean = true
	): String? {
		val logoTag = item?.imageTags?.get(ImageType.LOGO)
		return when {
			// No item
			item == null -> null
			// Item has a logo
			logoTag != null -> {
				api.imageApi.getItemImageUrl(
					itemId = item.id,
					imageType = ImageType.LOGO,
					maxWidth = maxWidth,
					tag = logoTag
				)
			}
			// Item parent has a logo
			item.parentLogoItemId != null && item.parentLogoImageTag != null -> {
				api.imageApi.getItemImageUrl(
					itemId = item.parentLogoItemId!!,
					imageType = ImageType.LOGO,
					maxWidth = maxWidth,
					tag = item.parentLogoImageTag
				)
			}
			// Series might have a logo
			useSeriesFallback && item.seriesId != null -> {
				api.imageApi.getItemImageUrl(
					itemId = item.seriesId!!,
					imageType = ImageType.LOGO,
					maxWidth = maxWidth,
				)
			}

			else -> null
		}
	}

	fun getThumbImageUrl(item: BaseItemDto, fillWidth: Int, fillHeight: Int): String {
		val thumbTag = item.imageTags?.get(ImageType.THUMB)

		return if (thumbTag == null) {
			getPrimaryImageUrl(item, true, fillWidth, fillHeight)
		} else {
			api.imageApi.getItemImageUrl(
				itemId = item.id,
				tag = thumbTag,
				imageType = ImageType.THUMB,
				fillWidth = fillWidth,
				fillHeight = fillHeight,
			)
		}
	}

	fun getBannerImageUrl(item: BaseItemDto, fillWidth: Int, fillHeight: Int): String {
		val bannerTag = item.imageTags?.get(ImageType.BANNER)

		return if (bannerTag == null) {
			getPrimaryImageUrl(item, true, fillWidth, fillHeight)
		} else {
			api.imageApi.getItemImageUrl(
				itemId = item.id,
				tag = bannerTag,
				imageType = ImageType.BANNER,
				fillWidth = fillWidth,
				fillHeight = fillHeight,
			)
		}
	}


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
