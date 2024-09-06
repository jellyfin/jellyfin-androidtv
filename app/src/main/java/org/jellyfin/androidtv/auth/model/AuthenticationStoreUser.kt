package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Locally stored user information. New properties require default values or deserialization will fail.
 */
@Serializable
data class AuthenticationStoreUser(
	val name: String,
	@SerialName("last_used") val lastUsed: Long = Instant.now().toEpochMilli(),
	@SerialName("image_tag") val imageTag: String? = null,
	@SerialName("access_token") val accessToken: String? = null,
)
