package org.jellyfin.playback.jellyfin

import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.jellyfin.mediastream.AudioMediaStreamResolver
import org.jellyfin.playback.jellyfin.mediastream.VideoMediaStreamResolver
import org.jellyfin.playback.jellyfin.playsession.PlaySessionService
import org.jellyfin.playback.jellyfin.playsession.PlaySessionSocketService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.EncodingContext
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.TranscodingProfile

fun jellyfinPlugin(
	api: ApiClient,
) = playbackPlugin {
	// TODO: Generate the device profile
	val profile = DeviceProfile(
		codecProfiles = emptyList(),
		containerProfiles = emptyList(),
		directPlayProfiles = emptyList(),
		subtitleProfiles = emptyList(),
		// Add at least one transcoding profile for both audio an video so the server returns a
		// value for "SupportsTranscoding" based on the user policy. We don't actually use this
		// profile in the client
		transcodingProfiles = listOf(
			TranscodingProfile(
				type = DlnaProfileType.AUDIO,
				context = EncodingContext.STREAMING,
				protocol = MediaStreamProtocol.HLS,
				container = "mp3",
				audioCodec = "mp3",
				videoCodec = "",
				conditions = emptyList()
			),
			TranscodingProfile(
				type = DlnaProfileType.VIDEO,
				context = EncodingContext.STREAMING,
				protocol = MediaStreamProtocol.HLS,
				container = "ts",
				audioCodec = "aac",
				videoCodec = "h264",
				conditions = emptyList()
			)
		),
	)
	provide(AudioMediaStreamResolver(api, profile))
	provide(VideoMediaStreamResolver(api, profile))

	val playSessionService = PlaySessionService(api)
	provide(playSessionService)
	provide(PlaySessionSocketService(api, playSessionService))
}
