package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults

@Composable
fun HomeToolbar(
	openSearch: () -> Unit,
	openSettings: () -> Unit,
	switchUsers: () -> Unit,
	userImage: String? = null,
) {
	Toolbar {
		ToolbarButtons {
			IconButton(onClick = openSearch) {
				Icon(
					painter = painterResource(R.drawable.ic_search),
					contentDescription = stringResource(R.string.lbl_search),
				)
			}

			IconButton(onClick = openSettings) {
				Icon(
					painter = painterResource(R.drawable.ic_settings),
					contentDescription = stringResource(R.string.lbl_settings),
				)
			}

			val userImagePainter = rememberAsyncImagePainter(userImage)
			val userImageState by userImagePainter.state.collectAsState()
			val userImageVisible = userImageState is AsyncImagePainter.State.Success
			IconButton(
				onClick = switchUsers,
				contentPadding = if (userImageVisible) PaddingValues(3.dp) else IconButtonDefaults.ContentPadding,
			) {
				if (userImageVisible) {
					Image(
						painter = userImagePainter,
						contentDescription = stringResource(R.string.lbl_switch_user),
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.aspectRatio(1f)
							.clip(IconButtonDefaults.Shape)
					)
				} else {
					Icon(
						painter = painterResource(R.drawable.ic_switch_users),
						contentDescription = stringResource(R.string.lbl_switch_user),
					)
				}
			}
		}
	}
}
