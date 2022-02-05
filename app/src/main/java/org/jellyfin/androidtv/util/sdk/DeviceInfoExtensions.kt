package org.jellyfin.androidtv.util.sdk

import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.UUID
import java.security.MessageDigest

fun DeviceInfo.legacy() = object : IDevice {
	override val deviceId: String get() = id
	override val deviceName: String get() = name
}

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
