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
	val url: String,
	@SerialName("last_used") val lastUsed: Long,
	@SerialName("display_order") val displayOrder: Int,
	val users: Map<UUID, AuthenticationStoreUser>,
)
