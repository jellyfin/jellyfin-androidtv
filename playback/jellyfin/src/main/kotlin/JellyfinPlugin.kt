package org.jellyfin.playback.jellyfin

import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.jellyfin.lyrics.LyricsPlayerService
import org.jellyfin.playback.jellyfin.mediasegment.MediaSegmentService
import org.jellyfin.playback.jellyfin.mediastream.JellyfinMediaStreamResolver
import org.jellyfin.playback.jellyfin.playsession.PlaySessionService
import org.jellyfin.playback.jellyfin.playsession.PlaySessionSocketService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.MediaSegmentType

fun jellyfinPlugin(
	api: ApiClient,
	deviceProfileBuilder: () -> DeviceProfile,
	mediaSegmentSkipTypes: Set<MediaSegmentType> = emptySet(),
	lifecycle: Lifecycle? = null,
) = playbackPlugin {
	// Provided as service as well to be able to use the queue for version selection
	val mediaStreamResolver = JellyfinMediaStreamResolver(api, deviceProfileBuilder)
	provide(mediaStreamResolver as PlayerService)
	provide(mediaStreamResolver as MediaStreamResolver)

	val playSessionService = PlaySessionService(api)
	provide(playSessionService)
	provide(PlaySessionSocketService(api, playSessionService, lifecycle))

	provide(LyricsPlayerService(api))

	if (mediaSegmentSkipTypes.isNotEmpty()) provide(MediaSegmentService(api, mediaSegmentSkipTypes))
}
