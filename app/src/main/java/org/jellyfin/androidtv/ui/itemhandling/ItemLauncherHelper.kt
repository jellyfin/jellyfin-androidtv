package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.java.KoinJavaComponent
import java.util.UUID

object ItemLauncherHelper {
	@JvmStatic
	fun launchUserView(context: Context, item: BaseItemDto) {
		val lifecycleOwner = context as? LifecycleOwner ?: ProcessLifecycleOwner.get()
		lifecycleOwner.lifecycleScope.launch {
			val navigationRepository by KoinJavaComponent.inject<NavigationRepository>(NavigationRepository::class.java)
			navigationRepository.navigate(getUserViewDestination(item))
		}
	}

	suspend fun getUserViewDestination(item: BaseItemDto): Destination.Fragment = when (item.collectionType) {
		CollectionType.MOVIES, CollectionType.TVSHOWS -> {
			val preferencesRepository by KoinJavaComponent.inject<PreferencesRepository>(PreferencesRepository::class.java)
			val preferences = preferencesRepository.loadLibraryPreferences(requireNotNull(item.displayPreferencesId))
			if (preferences[LibraryPreferences.enableSmartScreen]) Destinations.librarySmartScreen(item)
			else Destinations.libraryBrowser(item, null)
		}

		CollectionType.MUSIC, CollectionType.LIVETV -> Destinations.librarySmartScreen(item)
		else -> Destinations.libraryBrowser(item, null)
	}

	@JvmStatic
	fun getItem(itemId: UUID, callback: Response<BaseItemDto>) {
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val api by KoinJavaComponent.inject<ApiClient>(ApiClient::class.java)

			try {
				val response = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
				callback.onResponse(response)
			} catch (error: ApiClientException) {
				callback.onError(error)
			}
		}
	}
}
