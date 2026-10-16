package org.jellyfin.androidtv.ui.player.photo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.ui.composable.blurHashPainter
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

@Composable
fun PhotoPlayerContent(
	item: BaseItemDto?,
) {
	val api = koinInject<ApiClient>()
	val resources = LocalResources.current

	AnimatedContent(
		targetState = item,
		transitionSpec = {
			fadeIn() togetherWith fadeOut()
		}
	) { item ->
		val image = item?.itemImages[ImageType.PRIMARY]

		if (image?.blurHash != null) {
			Image(
				painter = blurHashPainter(image.blurHash, IntSize(32, 32)),
				contentDescription = null,
				alignment = Alignment.Center,
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxSize(),
			)
		}

		AsyncImage(
			model = image?.getUrl(
				api = api,
				maxWidth = resources.displayMetrics.widthPixels,
				maxHeight = resources.displayMetrics.heightPixels,
			),
			contentDescription = null,
			modifier = Modifier
				.fillMaxSize()
		)
	}
}
