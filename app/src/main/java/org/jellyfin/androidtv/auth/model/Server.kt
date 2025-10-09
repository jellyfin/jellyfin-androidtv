package org.jellyfin.androidtv.auth.model

import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.sdk.model.ServerVersion
import java.time.Instant
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
	val splashscreenEnabled: Boolean = false,
	val setupCompleted: Boolean = true,
	var dateLastAccessed: Instant = Instant.MIN,
) {
	val serverVersion = version?.let(ServerVersion::fromString)
	val versionSupported = serverVersion != null && serverVersion >= ServerRepository.minimumServerVersion

	operator fun compareTo(other: ServerVersion): Int = serverVersion?.compareTo(other) ?: -1

	override fun equals(other: Any?) = other is Server
		&& id == other.id
		&& address == other.address

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + address.hashCode()
		return result
	}
}
