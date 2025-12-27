package org.jellyfin.androidtv.ui.card

import android.content.Context
import androidx.compose.ui.unit.IntSize
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemKind

data class ImageCardUiState(
	val imageUrl: String?,
	val defaultIconRes: Int?,
	val overlayIconRes: Int? = null,
	val overlayText: String? = null,
	val overlayCount: String? = null,
	val title: String?,
	val contentText: String?,
	val unwatchedCount: Int,
	val progress: Int,
	val isFavorite: Boolean,
	val aspectRatio: Float
)

class ImageCardMapper(
	private val context: Context,
	private val imageHelper: ImageHelper
) {
	fun mapToUiState(
		item: BaseRowItem,
		imageType: ImageType,
		imageSize: IntSize
	): ImageCardUiState {
		return ImageCardUiState(
			imageUrl = item.getImageUrl(context, imageHelper, imageType, imageSize.width, imageSize.height),
			overlayIconRes = getOverlayIconRes(item),
			overlayText = item.getFullName(context),
			overlayCount = getOverlayCount(item),
			defaultIconRes = getDefaultIconRes(item),
			title = item.getCardName(context),
			contentText = item.getSubText(context),
			unwatchedCount = calculateUnwatchedCount(item),
			progress = calculateProgress(item),
			isFavorite = item.isFavorite,
			aspectRatio = getAspectRatio(item, imageType)
		)
	}

	private fun getOverlayCount(item: BaseRowItem): String? {
		return if (item is BaseItemDtoBaseRowItem) {
			item.childCountStr
		} else null

	}

	private fun calculateUnwatchedCount(item: BaseRowItem): Int {
		return when (item.baseItem?.type) {
			BaseItemKind.SERIES -> {
				item.baseItem.userData?.unplayedItemCount ?: -1
			}
			BaseItemKind.MOVIE -> {
				if (item.baseItem.userData?.played == true) 0 else -1
			}
			BaseItemKind.SEASON -> {
				item.baseItem.userData?.unplayedItemCount ?: -1
			}
			else -> -1
		}
	}

	private fun calculateProgress(item: BaseRowItem): Int {
		val position = item.baseItem?.userData?.playbackPositionTicks ?: 0
		val runtime = item.baseItem?.runTimeTicks ?: 0

		return if (runtime > 0) {
			((position.toDouble() / runtime) * 100).toInt()
		} else 0
	}

	private fun getAspectRatio(item: BaseRowItem, imageType: ImageType): Float {

		var aspect = 1.0

		when (item.baseRowType) {
			BaseRowType.BaseItem -> {
				when (imageType) {
					ImageType.BANNER -> {
						aspect = ImageHelper.ASPECT_RATIO_BANNER
					}
					ImageType.THUMB -> {
						aspect = ImageHelper.ASPECT_RATIO_16_9
					}
					else -> {
						aspect = ImageHelper.ASPECT_RATIO_2_3
					}
				}

				when (item.baseItem?.type) {

					BaseItemKind.AUDIO,
					BaseItemKind.MUSIC_ALBUM,
					BaseItemKind.MUSIC_ARTIST-> aspect = 1.0

					BaseItemKind.SEASON,
					BaseItemKind.SERIES-> {
						if (imageType == ImageType.POSTER) aspect = ImageHelper.ASPECT_RATIO_2_3
					}

					BaseItemKind.EPISODE-> {
						aspect = if (item is BaseItemDtoBaseRowItem && item.preferSeriesPoster) {
							ImageHelper.ASPECT_RATIO_2_3
						} else ImageHelper.ASPECT_RATIO_16_9
					}

					BaseItemKind.COLLECTION_FOLDER,
					BaseItemKind.USER_VIEW-> {
						aspect = ImageHelper.ASPECT_RATIO_16_9
					}

					BaseItemKind.MOVIE,
					BaseItemKind.VIDEO-> {
						if (imageType == ImageType.POSTER) aspect = ImageHelper.ASPECT_RATIO_2_3
					}

					else -> {
						if (imageType == ImageType.POSTER) aspect = ImageHelper.ASPECT_RATIO_2_3
					}
				}
			}

			else -> aspect = 2.0/3.0
		}

		return aspect.toFloat()
	}

	fun getDefaultIconRes(item: BaseRowItem): Int? {
		return when (item.baseRowType) {
			BaseRowType.BaseItem -> {
				when (item.baseItem?.type) {

					BaseItemKind.AUDIO,
					BaseItemKind.MUSIC_ALBUM-> R.drawable.ic_album

					BaseItemKind.PERSON,
					BaseItemKind.MUSIC_ARTIST-> R.drawable.ic_user

					BaseItemKind.SEASON,
					BaseItemKind.SERIES,
					BaseItemKind.EPISODE-> R.drawable.ic_tv

					BaseItemKind.COLLECTION_FOLDER,
					BaseItemKind.USER_VIEW,
					BaseItemKind.FOLDER,
					BaseItemKind.GENRE,
					BaseItemKind.MUSIC_GENRE,
					BaseItemKind.PHOTO_ALBUM,
					BaseItemKind.PLAYLIST-> R.drawable.ic_folder

					BaseItemKind.PHOTO -> R.drawable.ic_photo

					BaseItemKind.MOVIE,
					BaseItemKind.VIDEO-> R.drawable.ic_clapperboard

					else -> R.drawable.ic_folder
				}
			}
			BaseRowType.LiveTvChannel,
			BaseRowType.LiveTvProgram,
			BaseRowType.LiveTvRecording -> R.drawable.ic_tv

			BaseRowType.Person -> R.drawable.ic_user

			BaseRowType.Chapter,
			BaseRowType.GridButton -> R.drawable.ic_clapperboard

			BaseRowType.SeriesTimer -> R.drawable.ic_tv_timer
		}
	}

	private fun getOverlayIconRes(item: BaseRowItem): Int? {
		return when (item.baseItem?.type) {
			BaseItemKind.PHOTO -> R.drawable.ic_camera
			BaseItemKind.PHOTO_ALBUM -> R.drawable.ic_photos
			BaseItemKind.VIDEO -> R.drawable.ic_movie
			BaseItemKind.FOLDER -> R.drawable.ic_folder
			else -> null
		}
	}
}
