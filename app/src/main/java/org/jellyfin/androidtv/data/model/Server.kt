package org.jellyfin.androidtv.data.model

import java.util.*

/**
 * Server model to use locally in place of ServerInfo model in ApiClient.
 */
data class Server(
	var id: UUID,
	var name: String,
	var address: String,
	var dateLastAccessed: Date = Date(0)
) {
	override fun equals(other: Any?) = other is Server
		&& id == other.id
		&& address == other.address

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + address.hashCode()
		return result
	}
}
