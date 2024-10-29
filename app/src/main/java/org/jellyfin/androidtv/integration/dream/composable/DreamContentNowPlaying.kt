package org.jellyfin.androidtv.integration.dream.composable

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics
import org.jellyfin.playback.jellyfin.lyricsFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

@Composable
fun DreamContentNowPlaying(
	content: DreamContent.NowPlaying,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	val api = koinInject<ApiClient>()
	val playbackManager = koinInject<PlaybackManager>()
	val lyrics = content.entry.run { lyricsFlow.collectAsState(lyrics) }.value
	val progress = rememberPlayerProgress(playbackManager)

	val primaryImageTag = content.item.imageTags?.get(ImageType.PRIMARY)
	val (imageItemId, imageTag) = when {
		primaryImageTag != null -> content.item.id to primaryImageTag
		(content.item.albumId != null && content.item.albumPrimaryImageTag != null) -> content.item.albumId to content.item.albumPrimaryImageTag
		else -> null to null
	}

	// Background
	val imageBlurHash = imageTag?.let { tag ->
		content.item.imageBlurHashes?.get(ImageType.PRIMARY)?.get(tag)
	}
	if (imageBlurHash != null) {
		Image(
			painter = blurHashPainter(imageBlurHash, IntSize(32, 32)),
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize(),
		)

		DreamContentVignette()
	}

	// Lyrics overlay (on top of background)
	if (lyrics != null) {
		val playState by playbackManager.state.playState.collectAsState()
		LyricsDtoBox(
			lyricDto = lyrics,
			currentTimestamp = playbackManager.state.positionInfo.active,
			duration = playbackManager.state.positionInfo.duration,
			paused = playState != PlayState.PLAYING,
			fontSize = 22.sp,
			color = Color.White,
			modifier = Modifier
				.fillMaxSize()
				.fadingEdges(vertical = 250.dp)
				.padding(horizontal = 50.dp),
		)
	}

	// Metadata overlay (includes title / progress)
	Row(
		verticalAlignment = Alignment.Bottom,
		horizontalArrangement = Arrangement.spacedBy(20.dp),
		modifier = Modifier
			.align(Alignment.BottomStart)
			.overscan(),
	) {
		if (imageItemId != null) {
			AsyncImage(
				url = api.imageApi.getItemImageUrl(
					itemId = imageItemId,
					imageType = ImageType.PRIMARY,
					tag = imageTag,
					format = ImageFormat.WEBP,
				),
				blurHash = imageBlurHash,
				scaleType = ImageView.ScaleType.CENTER_CROP,
				modifier = Modifier
					.size(128.dp)
					.clip(RoundedCornerShape(5.dp))
			)
		}

		Column(
			modifier = Modifier
				.padding(bottom = 10.dp)
		) {
			Text(
				text = content.item.name.orEmpty(),
				style = TextStyle(
					color = Color.White,
					fontSize = 26.sp,
				),
			)

			Text(
				text = content.item.run {
					val artistNames = artists.orEmpty()
					val albumArtistNames = albumArtists?.mapNotNull { it.name }.orEmpty()

					when {
						artistNames.isNotEmpty() -> artistNames
						albumArtistNames.isNotEmpty() -> albumArtistNames
						else -> listOfNotNull(albumArtist)
					}.joinToString(", ")
				},
				style = TextStyle(
					color = Color(0.8f, 0.8f, 0.8f),
					fontSize = 18.sp,
				),
			)

			Spacer(modifier = Modifier.height(10.dp))

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
					.clip(RoundedCornerShape(2.dp))
					.drawWithContent {
						// Background
						drawRect(Color.White, alpha = 0.2f)
						// Foreground
						drawRect(
							Color.White,
							size = size.copy(width = progress * size.width)
						)
					}
			)
		}
	}
}
