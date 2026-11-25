package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID

@Composable
fun rememberLibraryPreferences(displayPreferencesId: String): LibraryPreferences? {
	val preferencesRepository = koinInject<PreferencesRepository>()
	return key(preferencesRepository, displayPreferencesId) {
		val preferences by produceState<LibraryPreferences?>(null) {
			value = preferencesRepository.loadLibraryPreferences(displayPreferencesId)
		}
		preferences
	}
}

@Composable
fun rememberBaseItemDto(itemId: UUID): BaseItemDto? {
	val api = koinInject<ApiClient>()
	return key(api, itemId) {
		val item by produceState<BaseItemDto?>(null) {
			value = try {
				withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
			} catch (error: ApiClientException) {
				Timber.w(error, "Failed to get display settings item $itemId")
				null
			}
		}
		item
	}
}
