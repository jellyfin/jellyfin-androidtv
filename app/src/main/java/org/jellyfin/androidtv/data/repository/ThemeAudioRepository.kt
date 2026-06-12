package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.userApi
import timber.log.Timber
import java.util.UUID

interface ThemeAudioRepository {
	suspend fun getThemeAudioUrl(itemId: UUID): String?
}

class ThemeAudioRepositoryImpl(private val api: ApiClient) : ThemeAudioRepository {

	override suspend fun getThemeAudioUrl(itemId: UUID): String? = withContext(Dispatchers.IO) {
		runCatching {
			val response = api.libraryApi.getThemeSongs(
				itemId = itemId,
				userId = api.userApi.getCurrentUser().content.id,
				inheritFromParent = true
			)

			val firstSongId = response.content.items.firstOrNull()?.id
				?: return@runCatching null

			val deviceId = api.deviceInfo.id
			"${api.baseUrl}/Audio/$firstSongId/stream?static=true&deviceId=$deviceId"
		}.onFailure { e ->
			Timber.e(e, "ThemeMediaRepository: Exception in getThemeAudioUrl for $itemId")
		}.getOrNull()
	}
}
