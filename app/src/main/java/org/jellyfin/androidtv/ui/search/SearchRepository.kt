package org.jellyfin.androidtv.ui.search

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.util.apiclient.AsyncState
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object SearchRepository: KoinComponent {

	private const val QUERY_LIMIT = 25

	private val apiClient: ApiClient by inject()

	suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>
	) = flow {
		emit(AsyncState.Loading)
		val searchHints = apiClient.itemsApi.getItemsByUserId(
			searchTerm = searchTerm,
			limit = QUERY_LIMIT,
			imageTypeLimit = 1,
			includeItemTypes = itemTypes,
			fields = listOf(
				ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
				ItemFields.CAN_DELETE,
				ItemFields.BASIC_SYNC_INFO,
				ItemFields.MEDIA_SOURCE_COUNT
			),
			recursive = true,
			enableTotalRecordCount = false,
		)
		emit(AsyncState.Success(searchHints))
	}.catch {
		emit(AsyncState.Error(it))
	}
}
