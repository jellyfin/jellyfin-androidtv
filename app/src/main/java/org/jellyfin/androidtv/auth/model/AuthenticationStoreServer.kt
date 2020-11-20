package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationStoreServer(
	val name: String,
	val url: String,
	@SerialName("last_used") val lastUsed: Long,
	@SerialName("display_order") val displayOrder: Int,
	val users: Map<@Contextual String, AuthenticationStoreUser>,
)
