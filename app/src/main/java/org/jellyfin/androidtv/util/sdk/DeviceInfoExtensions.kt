package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.UUID
import java.security.MessageDigest

fun DeviceInfo.forUser(userId: UUID) = forUser(userId.toString())

/**
 * @param user User ID or name
 */
fun DeviceInfo.forUser(user: String): DeviceInfo = copy(
	id = MessageDigest.getInstance("SHA-1").run {
		update("${id}+$user".toByteArray())
		digest().joinToString("") { "%02x".format(it) }
	},
)
