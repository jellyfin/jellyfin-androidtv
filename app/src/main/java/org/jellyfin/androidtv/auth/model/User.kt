package org.jellyfin.androidtv.auth.model

import java.util.*

/**
 * User model to use locally in place of UserDto model in ApiClient.
 */
data class User(
	val id: UUID,
	val serverId: UUID,
	val name: String,
	val accessToken: String?,
) {
	override fun equals(other: Any?) = other is User
		&& serverId == other.serverId
		&& id == other.id

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + serverId.hashCode()
		return result
	}
}
