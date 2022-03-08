package org.jellyfin.androidtv.auth.model

import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.sdk.model.ServerVersion
import java.util.Date
import java.util.UUID

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
	val serverVersion = version?.let(ServerVersion::fromString)
	val versionSupported = serverVersion != null && serverVersion >= ServerRepository.minimumServerVersion


	fun isVersionEqualOrGreater(otherVersion: ServerVersion?): Boolean {
		val serverVersion: ServerVersion? = version?.let { ServerVersion.fromString(it) }
		val result = otherVersion?.let { serverVersion?.compareTo(it) } ?: -1
		return result >= 0
	}

	override fun equals(other: Any?) = other is Server
		&& id == other.id
		&& address == other.address

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + address.hashCode()
		return result
	}
}
