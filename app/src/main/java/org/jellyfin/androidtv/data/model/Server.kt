package org.jellyfin.androidtv.data.model

import java.util.*

/**
 * Server model to use locally in place of ServerInfo model in ApiClient.
 */
open class Server(
	var id: String,
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

@Deprecated("Should use Server model that does not include legacy user fields")
class LegacyServer(
	id: String,
	name: String,
	address: String,
	dateLastAccessed: Date = Date(0),
	var userId: String? = null,
	var accessToken: String? = null
) : Server(
	id,
	name,
	address,
	dateLastAccessed
)
