package org.jellyfin.androidtv.ui.jellyseerr

import org.jellyfin.androidtv.data.repository.JellyseerrRequest
import org.jellyfin.androidtv.data.repository.JellyseerrSearchItem

internal object JellyseerrRequestMarkers {
	fun markItemsWithRequests(
		items: List<JellyseerrSearchItem>,
		requests: List<JellyseerrRequest>,
	): List<JellyseerrSearchItem> = items.map { item ->
		val match = requests.firstOrNull { it.tmdbId == item.id }
		val requestStatus = match?.status
		val hasPendingRequest = requestStatus != null && requestStatus != 5
		val availableFromRequest = requestStatus == 5

		item.copy(
			isRequested = hasPendingRequest,
			isAvailable = item.isAvailable || availableFromRequest,
			requestId = match?.id ?: item.requestId,
			requestStatus = requestStatus ?: item.requestStatus,
			isPartiallyAvailable = item.isPartiallyAvailable,
		)
	}

	fun markSelectedItemWithRequests(
		selectedItem: JellyseerrSearchItem?,
		requests: List<JellyseerrRequest>,
	): JellyseerrSearchItem? {
		selectedItem ?: return null

		val match = requests.firstOrNull { it.tmdbId == selectedItem.id }
		val requestStatus = match?.status
		val hasPendingRequest = requestStatus != null && requestStatus != 5
		val availableFromRequest = requestStatus == 5

		return selectedItem.copy(
			isRequested = hasPendingRequest,
			isAvailable = selectedItem.isAvailable || availableFromRequest,
			requestId = match?.id ?: selectedItem.requestId,
			requestStatus = requestStatus ?: selectedItem.requestStatus,
			isPartiallyAvailable = selectedItem.isPartiallyAvailable,
		)
	}
}
