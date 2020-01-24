package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object PlaybackHelperKt {
	suspend fun getItemsToPlayCoroutine(mainItem: BaseItemDto, allowIntros: Boolean, shuffle: Boolean): List<BaseItemDto>? = suspendCoroutine { continuation ->
		PlaybackHelper.getItemsToPlay(mainItem, allowIntros, shuffle, object : Response<List<BaseItemDto>>() {
			override fun onResponse(response: List<BaseItemDto>?) {
				continuation.resume(response!!)
			}

			override fun onError(exception: Exception?) {
				continuation.resume(null)
			}
		})
	}
}
