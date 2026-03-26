package org.jellyfin.androidtv.ui.browsing

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import java.util.UUID
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.model.api.ItemSortBy

fun BrowseGridFragment.createSettingsVisibility() = MutableStateFlow(false)

fun BrowseGridFragment.addSettings(
	view: ComposeView,
	itemId: UUID,
	displayPreferencesId: String,
	visible: MutableStateFlow<Boolean>,
) {
	view.setContent {
		val isVisible by visible.collectAsState(false)

		ProvideRouter(
			routes,
			Routes.LIBRARIES_DISPLAY,
			mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
		) {
			SettingsDialog(
				visible = isVisible,
				onDismissRequest = {
					visible.value = false
					onResume()
				}
			) {
				SettingsRouterContent()
			}
		}
	}
}

fun loadGenreNames(
	lifecycle: Lifecycle,
	apiClient: ApiClient,
	parentId: UUID?,
	onLoaded: Consumer<List<String>>,
	onError: Consumer<Throwable>,
) {
	lifecycle.coroutineScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				apiClient.genresApi.getGenres(
					parentId = parentId,
					sortBy = setOf(ItemSortBy.SORT_NAME),
				).content.items
					.mapNotNull { it.name?.takeIf(String::isNotBlank) }
					.distinct()
			}
		}.onSuccess(onLoaded::accept)
			.onFailure(onError::accept)
	}
}
