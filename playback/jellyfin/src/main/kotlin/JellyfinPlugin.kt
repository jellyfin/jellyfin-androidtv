package org.jellyfin.playback.jellyfin

import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.jellyfin.mediastream.UniversalAudioMediaStreamResolver
import org.jellyfin.playback.jellyfin.playsession.PlaySessionService
import org.jellyfin.playback.jellyfin.playsession.PlaySessionSocketService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.sockets.SocketInstance
import org.jellyfin.sdk.model.api.DeviceProfile

fun jellyfinPlugin(
	api: ApiClient,
	socketInstance: SocketInstance,
) = playbackPlugin {
	// TODO: Generate the device profile
	// The current resolver doesn't use it yet so we can get away with this empty one for now
	val profile = DeviceProfile(
		codecProfiles = emptyList(),
		containerProfiles = emptyList(),
		directPlayProfiles = emptyList(),
		responseProfiles = emptyList(),
		subtitleProfiles = emptyList(),
		supportedMediaTypes = "",
		transcodingProfiles = emptyList(),
		xmlRootAttributes = emptyList(),
	)
	provide(UniversalAudioMediaStreamResolver(api, profile))

	val playSessionService = PlaySessionService(api)
	provide(playSessionService)
	provide(PlaySessionSocketService(socketInstance, playSessionService))
}
