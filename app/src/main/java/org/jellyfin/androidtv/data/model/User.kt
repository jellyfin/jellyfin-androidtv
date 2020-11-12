package org.jellyfin.androidtv.data.model

/**
 * User model to use locally in place of UserDto model in ApiClient.
 */
data class User(
	val id: String,
	val name: String,
	val accessToken: String = "",
	val serverId: String,
	val primaryImageTag: String = "",
	val hasPassword: Boolean = true,
	val hasConfiguredPassword: Boolean = true,
	val hasConfiguredEasyPassword: Boolean = false,
	val configuration: UserConfiguration = UserConfiguration(),
	val policy: UserPolicy = UserPolicy()
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
