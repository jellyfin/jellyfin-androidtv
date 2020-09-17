package org.jellyfin.androidtv.data.model

import java.util.*

/**
 * Server model to use locally in place of ServerInfo model in ApiClient.
 */
data class Server(
	var id: String,
	var name: String,
	var address: String,
	@Deprecated("This is only used for legacy stored credentials")
	val userId: String? = null,
	@Deprecated("This is only used for legacy stored credentials")
	val accessToken: String? = null,
	val dateLastAccessed: Date = Date(0)
) {
	override fun equals(other: Any?) = (other is Server) && id == other.id

	override fun hashCode() = id.hashCode()
}
