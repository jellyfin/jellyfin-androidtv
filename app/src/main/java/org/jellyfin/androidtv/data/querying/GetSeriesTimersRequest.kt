package org.jellyfin.androidtv.data.querying

import java.util.UUID

data class GetSeriesTimersRequest(
	val parentId: UUID? = null,
)
