package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationStoreUser(
	val name: String,
	val profile_picture: String,
	@SerialName("last_used") val lastUsed: Long,
	@SerialName("display_order") val displayOrder: Int,
)
