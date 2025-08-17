package org.jellyfin.androidtv.ui.playback

import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.player.base.PlayerSeekbar
import org.jellyfin.androidtv.util.apiclient.albumPrimaryImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics
import org.jellyfin.playback.jellyfin.lyricsFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

fun initializePlayerProgress(
	playerProgress: ComposeView,
	playbackManager: PlaybackManager,
) {
	playerProgress.setContent {
		Box(
			modifier = Modifier
				.padding(horizontal = 10.dp, vertical = 20.dp)
		) {
			PlayerSeekbar(
				playbackManager = playbackManager,
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
			)
		}
	}
}

fun initializePreviewView(
	lyricsView: ComposeView,
	playbackManager: PlaybackManager,
) {
	lyricsView.setContent {
		val api = koinInject<ApiClient>()
		val entry by rememberQueueEntry(playbackManager)
		val baseItem = entry?.run { baseItemFlow.collectAsState(baseItem).value }
		val lyrics = entry?.run { lyricsFlow.collectAsState(lyrics) }?.value
		val cover = baseItem?.itemImages[ImageType.PRIMARY] ?: baseItem?.albumPrimaryImage ?: baseItem?.parentImages[ImageType.PRIMARY]

		// Show track/album art when available and fade it out when lyrics are displayed on top
		val coverViewAlpha by animateFloatAsState(
			label = "coverViewAlpha",
			targetValue = if (lyrics == null) 1f else 0.2f,
		)

		AnimatedContent(cover) { cover ->
			if (cover != null) {
				Box(
					modifier = Modifier
						.wrapContentSize()
						.clip(RoundedCornerShape(4.dp))
						.background(Color.Black)
				) {
					AsyncImage(
						url = cover.getUrl(api),
						blurHash = cover.blurHash,
						aspectRatio = cover.aspectRatio?.toFloat() ?: 1f,
						scaleType = ImageView.ScaleType.CENTER_INSIDE,
						modifier = Modifier
							.alpha(coverViewAlpha)
					)
				}
			} else if (lyrics == null) {
				// "placeholder" image
				Icon(ImageVector.vectorResource(R.drawable.ic_album), contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
			}
		}

		// Display lyrics overlay
		if (lyrics != null) {
			val playState by remember { playbackManager.state.playState }.collectAsState()

			// Using the progress animation causes the layout to recompose, which we need for synced lyrics to work
			// we don't actually use the animation value here
			rememberPlayerProgress(playbackManager)

			LyricsDtoBox(
				lyricDto = lyrics,
				currentTimestamp = playbackManager.state.positionInfo.active,
				duration = playbackManager.state.positionInfo.duration,
				paused = playState != PlayState.PLAYING,
				fontSize = 12.sp,
				color = Color.White,
				modifier = Modifier
					.fillMaxSize()
					.fadingEdges(vertical = 50.dp)
					.padding(horizontal = 15.dp),
			)
		}
	}
}
