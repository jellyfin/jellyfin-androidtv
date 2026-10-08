@file:UseSerializers(UUIDSerializer::class)

package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import java.time.Instant
import java.util.UUID

/**
 * Locally stored server information. New properties require default values or deserialization will fail.
 */
@Serializable
data class AuthenticationStoreServer(
	val name: String,
	val address: String,
	val version: String? = null,
	@SerialName("login_disclaimer")  val loginDisclaimer: String? = null,
	@SerialName("splashscreen_enabled")  val splashscreenEnabled: Boolean = false,
	@SerialName("setup_completed")  val setupCompleted: Boolean = true,
	@SerialName("last_used") val lastUsed: Long = Instant.now().toEpochMilli(),
	@SerialName("last_refreshed") val lastRefreshed: Long = Instant.now().toEpochMilli(),
	/**
	 * Custom HTTP headers to send with every request to this server, e.g. an API key or a token
	 * required by an authenticating reverse proxy or identity-aware gateway (such as Authentik or
	 * Authelia) placed in front of the server.
	 */
	@SerialName("custom_headers") val customHeaders: Map<String, String> = emptyMap(),
	val users: Map<UUID, AuthenticationStoreUser> = emptyMap(),
)
