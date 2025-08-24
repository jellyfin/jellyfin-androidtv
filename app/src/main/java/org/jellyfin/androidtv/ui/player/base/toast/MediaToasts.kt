package org.jellyfin.androidtv.ui.player.base.toast

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.jellyfin.androidtv.ui.base.Icon

@Composable
fun MediaToasts(mediaToastRegistry: MediaToastRegistry) {
	val data by mediaToastRegistry.current.collectAsState()

	MediaToast(
		visible = data != null,
		icon = {
			data?.icon?.let { icon ->
				Icon(
					imageVector = ImageVector.vectorResource(icon),
					contentDescription = null,
					modifier = Modifier.fillMaxSize()
				)
			}
		},
		progress = data?.progress,
	)
}
