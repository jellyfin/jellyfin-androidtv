package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthenticationStoreUser(
	val name: String,
	@SerialName("last_used") val lastUsed: Long = Date().time,
)
