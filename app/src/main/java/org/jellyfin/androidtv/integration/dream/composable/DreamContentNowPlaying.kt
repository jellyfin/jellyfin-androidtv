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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.ui.composable.overscan
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DreamContentNowPlaying(
	content: DreamContent.NowPlaying,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	val api = koinInject<ApiClient>()
	val mediaManager = koinInject<MediaManager>()
	val item = content.item ?: return@Box

	val primaryImageTag = item.imageTags?.get(ImageType.PRIMARY)
	val (imageItemId, imageTag) = when {
		primaryImageTag != null -> item.id to primaryImageTag
		(item.albumId != null && item.albumPrimaryImageTag != null) -> item.albumId to item.albumPrimaryImageTag
		else -> null to null
	}

	val imageBlurHash = imageTag?.let { tag -> item.imageBlurHashes?.get(ImageType.PRIMARY)?.get(tag) }
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

	// Overlay
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
				text = item.name.orEmpty(),
				style = TextStyle(
					color = Color.White,
					fontSize = 26.sp,
				),
			)

			Text(
				text = item.run {
					if (!artists.isNullOrEmpty()) return@run artists?.joinToString(", ")
					val albumArtistNames = albumArtists?.mapNotNull { it.name }
					if (!albumArtistNames.isNullOrEmpty()) return@run albumArtistNames.joinToString(", ")
					return@run albumArtist
				}.orEmpty(),
				style = TextStyle(
					color = Color(0.8f, 0.8f, 0.8f),
					fontSize = 18.sp,
				),
			)

			Spacer(modifier = Modifier.height(10.dp))

			var progress by remember { mutableFloatStateOf(0f) }
			DisposableEffect(Unit) {
				val listener = object : AudioEventListener {
					override fun onProgress(pos: Long) {
						val duration = item.runTimeTicks?.ticks ?: Duration.ZERO
						progress = (pos.milliseconds / duration).toFloat()
					}
				}

				mediaManager.addAudioEventListener(listener)

				onDispose {
					mediaManager.removeAudioEventListener(listener)
				}
			}

			LinearProgressIndicator(
				progress = progress,
				color = Color.White,
				backgroundColor = Color.White.copy(alpha = 0.2f),
				strokeCap = StrokeCap.Round,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}
