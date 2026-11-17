package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
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
fun rememberUserView(itemId: UUID): BaseItemDto? {
	val userViewsRepository = koinInject<UserViewsRepository>()
	val userView by remember {
		userViewsRepository.views.map { views -> views.first { view -> view.id == itemId } }
	}.collectAsState(null)
	return userView
}
