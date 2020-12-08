package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthenticationStoreUser(
	val name: String,
	//TODO remove? We can just use /Users/{id}/Images/Primary
	@SerialName("profile_picture") val profilePictureTag: String?,
	@SerialName("last_used") val lastUsed: Long = Date().time,
)
