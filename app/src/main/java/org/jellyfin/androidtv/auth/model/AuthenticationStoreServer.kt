@file:UseSerializers(UUIDSerializer::class)

package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jellyfin.androidtv.util.serializer.UUIDSerializer
import java.util.*

/**
 * Locally stored server information. New properties require default values or deserialization will fail.
 */
@Serializable
data class AuthenticationStoreServer(
	val name: String,
	val address: String,
	val version: String? = null,
	@SerialName("login_disclaimer")  val loginDisclaimer: String? = null,
	@SerialName("last_used") val lastUsed: Long = Date().time,
	@SerialName("last_refreshed") val lastRefreshed: Long = Date().time,
	val users: Map<UUID, AuthenticationStoreUser> = emptyMap(),
)
