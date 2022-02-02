package org.jellyfin.androidtv.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.androidtv.util.apiclient.callApiEmpty
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.entities.MediaType
import org.jellyfin.apiclient.model.session.ClientCapabilities
import org.jellyfin.apiclient.model.session.GeneralCommandType
import timber.log.Timber

class ApiBinder(
	private val application: JellyfinApplication,
	private val api: ApiClient,
	private val device: IDevice,
	private val authenticationStore: AuthenticationStore,
) {
	fun updateSession(session: Session?, resultCallback: ((Boolean) -> Unit)? = null) {
		GlobalScope.launch(Dispatchers.IO) {
			@Suppress("TooGenericExceptionCaught")
			val success = try {
				updateSessionInternal(session)
			} catch (throwable: Throwable) {
				Timber.e(throwable, "Unable to update legacy API session.")
				false
			}

			resultCallback?.invoke(success)
		}
	}

	private suspend fun updateSessionInternal(session: Session?): Boolean {
		if (session == null) {
			application.currentUser = null
			return true
		}

		val server = authenticationStore.getServer(session.serverId)
		if (server == null) {
			Timber.e("Could not bind API because server ${session.serverId} was not found in the store.")
			return false
		}

		api.device = AuthenticationDevice(device, session.userId.toString())
		api.SetAuthenticationInfo(session.accessToken, session.userId.toString())
		api.EnableAutomaticNetworking(ServerInfo().apply {
			id = session.serverId.toString()
			name = server.name
			address = server.address.removeSuffix("/")
			userId = session.userId.toString()
			accessToken = session.accessToken
		})

		// Update currentUser DTO
		val user = callApi<UserDto> { callback -> api.GetUserAsync(session.userId.toString(), callback) }
		application.currentUser = user

		callApiEmpty { callback ->
			api.ReportCapabilities(ClientCapabilities().apply {
				playableMediaTypes = arrayListOf(MediaType.Video, MediaType.Audio)
				supportsMediaControl = true
				supportedCommands = arrayListOf(
					GeneralCommandType.DisplayContent.toString(),
					GeneralCommandType.DisplayMessage.toString(),
				)
			}, callback)
		}

		// Connect to WebSocket AFTER HTTP connection confirmed working
		// to catch exceptions not catchable with the legacy websocket client
		api.ensureWebSocket()

		return true
	}
}
