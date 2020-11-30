@file:UseSerializers(UUIDSerializer::class)

package org.jellyfin.androidtv.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jellyfin.androidtv.util.serializer.UUIDSerializer
import java.util.*

@Serializable
data class AuthenticationStoreServer(
	val name: String,
	val address: String,
	@SerialName("last_used") val lastUsed: Long = Date().time,
	val users: Map<UUID, AuthenticationStoreUser> = emptyMap(),
)
