package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.player.base.PlayerHeader
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

@Composable
@Stable
fun VideoPlayerHeader(
	item: BaseItemDto?,
	modifier: Modifier = Modifier,
) {
	val api = koinInject<ApiClient>()
	val logo = item?.itemImages?.get(ImageType.LOGO) ?: item?.parentImages?.get(ImageType.LOGO)
	val logoPainter = rememberAsyncImagePainter(logo?.getUrl(api, maxWidth = 440))
	val logoState by logoPainter.state.collectAsState()
	val logoLoaded = logoState is AsyncImagePainter.State.Success

	PlayerHeader(modifier = modifier) {
		if (item != null) {
			if (logoLoaded) {
				Image(
					painter = logoPainter,
					contentDescription = item.name,
					contentScale = ContentScale.Fit,
					modifier = Modifier
						.height(60.dp)
						.widthIn(max = 450.dp),
				)
			} else {
				Text(
					text = item.name.orEmpty(),
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					style = LocalTextStyle.current.copy(
						color = Color.White,
						fontSize = 22.sp
					)
				)
			}

			Text(
				text = item.seriesName.orEmpty(),
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				style = LocalTextStyle.current.copy(
					color = Color.White,
					fontSize = 18.sp
				)
			)
		}
	}
}
