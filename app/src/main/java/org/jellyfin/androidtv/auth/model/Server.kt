package org.jellyfin.androidtv.auth.model

import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.sdk.model.ServerVersion
import java.util.*

/**
 * Server model to use locally in place of ServerInfo model in ApiClient.
 */
data class Server(
	var id: UUID,
	var name: String,
	var address: String,
	val version: String? = null,
	val loginDisclaimer: String? = null,
	var dateLastAccessed: Date = Date(0),
) {
	private val serverVersion = version?.let(ServerVersion::fromString)
	val versionSupported = serverVersion != null && serverVersion >= ServerRepository.minimumServerVersion

	override fun equals(other: Any?) = other is Server
		&& id == other.id
		&& address == other.address

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + address.hashCode()
		return result
	}
}
