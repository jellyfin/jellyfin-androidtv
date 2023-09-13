package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent
import java.util.UUID

object ItemLauncherHelper {
	@JvmStatic
	fun getItem(itemId: UUID, callback: Response<BaseItemDto>) {
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val api by KoinJavaComponent.inject<ApiClient>(ApiClient::class.java)

			try {
				val response by api.userLibraryApi.getItem(itemId = itemId)
				callback.onResponse(response)
			} catch (error: ApiClientException) {
				callback.onError(error)
			}
		}
	}
}
