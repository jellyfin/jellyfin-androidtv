package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

/**
 * The items to play and the media source to use for the item playback was started for.
 *
 * An alternate version is not always an item of the queue itself: it is not part of the episode
 * listing of its series, so the queue contains its primary episode instead and [mediaSourceId]
 * selects the version to play for it.
 *
 * @param items The items to play.
 * @param mediaSourceId Id of the media source to play for the item playback was started for, or null
 * to let the server pick the media source.
 */
data class ItemsToPlay(
	val items: List<BaseItemDto>,
	val mediaSourceId: String? = null,
)

interface PlaybackHelper {
	fun getItemsToPlay(
		context: Context,
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
		outerResponse: Response<ItemsToPlay>,
	)

	fun retrieveAndPlay(itemId: UUID, shuffle: Boolean, context: Context)

	fun retrieveAndPlay(itemIds: List<UUID>, shuffle: Boolean, position: Long?, index: Int?, context: Context)

	fun playInstantMix(context: Context, item: BaseItemDto)
}
