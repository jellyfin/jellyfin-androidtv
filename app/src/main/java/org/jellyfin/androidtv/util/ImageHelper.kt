package org.jellyfin.androidtv.util

import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.serializer.toUUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.jellyfin.apiclient.model.dto.UserDto as LegacyUserDto

class ImageHelper(
	private val api: ApiClient,
) {
	fun getPrimaryImageUrl(item: BaseItemPerson, maxHeight: Int? = null): String? {
		if (item.primaryImageTag == null) return null

		return item.id?.toUUIDOrNull()?.let { itemId ->
			api.imageApi.getItemImageUrl(
				itemId = itemId,
				imageType = ImageType.PRIMARY,
				tag = item.primaryImageTag,
				maxHeight = maxHeight,
			)
		}
	}

	fun getPrimaryImageUrl(item: LegacyUserDto): String? {
		if (item.primaryImageTag == null) return null

		return item.id?.toUUIDOrNull()?.let { userId ->
			api.imageApi.getUserImageUrl(
				userId = userId,
				imageType = ImageType.PRIMARY,
				tag = item.primaryImageTag,
				maxHeight = ImageUtils.MAX_PRIMARY_IMAGE_HEIGHT,
			)
		}
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

	fun getImageUrl(item: String, imageType: ImageType, imageTag: String): String? = item.toUUIDOrNull()?.let { itemId ->
		api.imageApi.getItemImageUrl(
			itemId = itemId,
			imageType = imageType,
			tag = imageTag,
			maxHeight = ImageUtils.MAX_PRIMARY_IMAGE_HEIGHT,
		)
	}

	fun getPrimaryImageUrl(item: BaseItemDto, preferParentThumb: Boolean, maxHeight: Int): String {
		var itemId = item.id
		var imageTag = item.imageTags?.get(ImageType.PRIMARY)
		var imageType = ImageType.PRIMARY

		if (preferParentThumb && item.type.equals(BaseItemType.Episode.toString(), ignoreCase = true)) {
			if (item.parentThumbItemId != null && item.parentThumbImageTag != null) {
				itemId = item.parentThumbItemId!!.toUUID()
				imageTag = item.parentThumbImageTag
				imageType = ImageType.THUMB
			} else if (item.seriesId != null && item.seriesThumbImageTag != null) {
				itemId = item.seriesId!!
				imageTag = item.seriesThumbImageTag
				imageType = ImageType.THUMB
			}
		} else if (item.type.equals(BaseItemType.Season.toString(), ignoreCase = true) && imageTag == null) {
			if (item.seriesId != null && item.seriesPrimaryImageTag != null) {
				itemId = item.seriesId!!
				imageTag = item.seriesPrimaryImageTag
			}
		} else if (item.type.equals(BaseItemType.Program.toString(), ignoreCase = true) && item.imageTags?.containsKey(ImageType.THUMB) == true) {
			imageTag = item.imageTags!![ImageType.THUMB]
			imageType = ImageType.THUMB
		} else if (item.type.equals(BaseItemType.Audio.toString(), ignoreCase = true) && imageTag == null) {
			if (item.albumId != null && item.albumPrimaryImageTag != null) {
				itemId = item.albumId!!
				imageTag = item.albumPrimaryImageTag
			} else if (!item.albumArtists.isNullOrEmpty()) {
				itemId = item.albumArtists!!.first().id
				imageTag = null
			}
		}

		return api.imageApi.getItemImageUrl(
			itemId = itemId,
			imageType = imageType,
			tag = imageTag,
			maxHeight = maxHeight,
		)
	}

	fun getLogoImageUrl(item: BaseItemDto?, maxWidth: Int? = null, useSeriesFallback: Boolean = true): String? {
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
					itemId = item.parentLogoItemId!!.toUUID(),
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
}
