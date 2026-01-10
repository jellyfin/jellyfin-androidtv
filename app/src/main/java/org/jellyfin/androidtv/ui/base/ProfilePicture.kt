package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R

@Composable
fun ProfilePicture(
	url: String?,
	modifier: Modifier = Modifier,
	contentDescription: String? = null,
	iconPadding: PaddingValues = PaddingValues.Zero,
) {
	Box(
		modifier = modifier
	) {
		val userImagePainter = rememberAsyncImagePainter(url)
		val userImageState by userImagePainter.state.collectAsState()
		val userImageVisible = userImageState is AsyncImagePainter.State.Success

		if (!userImageVisible) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_user),
				contentDescription = contentDescription,
				modifier = Modifier
					.align(Alignment.Center)
					.padding(iconPadding)
					.fillMaxSize()
			)
		} else {
			Image(
				painter = userImagePainter,
				contentDescription = contentDescription,
				contentScale = ContentScale.Crop,
				modifier = Modifier
					.aspectRatio(1f)
			)
		}
	}
}
