package org.jellyfin.androidtv.auth.apiclient

import org.jellyfin.androidtv.auth.repository.Session
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import org.jellyfin.androidtv.util.sdk.legacy
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.sdk.model.DeviceInfo
import timber.log.Timber

class ApiBinder(
	private val api: ApiClient,
	private val authenticationStore: AuthenticationStore,
) {
	fun updateSession(session: Session?, deviceInfo: DeviceInfo): Boolean {
		@Suppress("TooGenericExceptionCaught")
		val success = try {
			updateSessionInternal(session, deviceInfo)
		} catch (throwable: Throwable) {
			Timber.e(throwable, "Unable to update legacy API session.")
			false
		}

		return success
	}

	private fun updateSessionInternal(session: Session?, deviceInfo: DeviceInfo): Boolean {
		if (session == null) return true

		val server = authenticationStore.getServer(session.serverId)
		if (server == null) {
			Timber.e("Could not bind API because server ${session.serverId} was not found in the store.")
			return false
		}

		api.device = deviceInfo.legacy()
		api.SetAuthenticationInfo(session.accessToken, session.userId.toString())
		api.EnableAutomaticNetworking(ServerInfo().apply {
			id = session.serverId.toString()
			name = server.name
			address = server.address.removeSuffix("/")
			userId = session.userId.toString()
			accessToken = session.accessToken
		})

		return true
	}
}
