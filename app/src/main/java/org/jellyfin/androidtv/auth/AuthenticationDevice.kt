package org.jellyfin.androidtv.auth

import org.jellyfin.apiclient.interaction.device.IDevice
import java.security.MessageDigest

/**
 * Wrapper for [IDevice] that changes the device id to a combination of the original device id
 * and the username. This is neccasary to prevent the server from trashing other sessions with
 * the same device id.
 */
class AuthenticationDevice(
	private val parent: IDevice,
	private val username: String
) : IDevice {
	// Hash the deviceId because we can't use special characters as the device id is send
	// via HTTP headers
	override val deviceId = MessageDigest.getInstance("SHA-1").run {
		update("${parent.deviceId}+${username}".toByteArray())
		digest().joinToString("") { "%02x".format(it) }
	}

	override val deviceName = parent.deviceName
}
