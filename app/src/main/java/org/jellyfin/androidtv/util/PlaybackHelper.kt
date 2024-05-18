package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

interface PlaybackHelper {
	fun getItemsToPlay(
		context: Context,
		mainItem: BaseItemDto,
		allowIntros: Boolean,
		shuffle: Boolean,
		outerResponse: Response<List<BaseItemDto>>,
	)

	fun retrieveAndPlay(id: UUID, shuffle: Boolean, position: Long?, context: Context)

	fun retrieveAndPlay(id: UUID, shuffle: Boolean, context: Context) =
		retrieveAndPlay(id, shuffle, null, context)

	fun playInstantMix(context: Context, item: BaseItemDto)
}
