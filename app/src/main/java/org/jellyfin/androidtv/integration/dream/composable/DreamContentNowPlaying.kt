package org.jellyfin.androidtv.integration.dream.composable

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanniktech.blurhash.BlurHash
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.overscan
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.ImageFormat
import org.jellyfin.sdk.model.api.ImageType
import org.koin.androidx.compose.get

@Composable
fun DreamContentNowPlaying(
	content: DreamContent.NowPlaying,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	val api = get<ApiClient>()
	val item = content.item ?: return@Box

	val primaryImageTag = item.imageTags?.get(ImageType.PRIMARY)
	val (imageItemId, imageTag) = when {
		primaryImageTag != null -> item.id to primaryImageTag
		(item.albumId != null && item.albumPrimaryImageTag != null) -> item.albumId to item.albumPrimaryImageTag
		else -> null to null
	}
	val imageBlurHash = imageTag?.let { tag -> item.imageBlurHashes?.get(ImageType.PRIMARY)?.get(tag) }

	val imageBlurHashBitmap = remember {
		if (imageBlurHash != null) BlurHash.decode(imageBlurHash, 32, 32)?.asImageBitmap()
		else null
	}

	if (imageBlurHashBitmap != null) {
		Image(
			bitmap = imageBlurHashBitmap,
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize(),
			colorFilter = ColorFilter.tint(Color(0f, 0f, 0f, 0.4f), BlendMode.SrcAtop)
		)
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
			modifier = Modifier.padding(bottom = 10.dp)
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
		}
	}
}
