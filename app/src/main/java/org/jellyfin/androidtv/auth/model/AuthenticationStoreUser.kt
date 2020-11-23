package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationStoreUser(
	val name: String,
	@SerialName("profile_picture") val profilePicture: String,
	@SerialName("last_used") val lastUsed: Long,
	@SerialName("display_order") val displayOrder: Int,
)
