package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.util.apiclient.Response
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

	fun retrieveAndPlay(id: List<UUID>, shuffle: Boolean, position: Long?, index: Int?, context: Context)

	fun retrieveAndPlay(id: UUID, shuffle: Boolean, context: Context) =
		retrieveAndPlay(listOf(id), shuffle, null, null, context)

	fun playInstantMix(context: Context, item: BaseItemDto)
}
