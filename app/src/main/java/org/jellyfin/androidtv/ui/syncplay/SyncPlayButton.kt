package org.jellyfin.androidtv.ui.syncplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayService
import org.koin.compose.koinInject

@Composable
fun SyncPlayButton(
	onClick: () -> Unit,
	playbackManager: PlaybackManager = koinInject(),
) {
	val service = playbackManager.getService<SyncPlayService>() ?: return
	val group by service.group.collectAsState()

	IconButton(
		onClick = onClick,
		colors = if (group != null) {
			ButtonDefaults.colors(
				containerColor = JellyfinTheme.colorScheme.buttonActive,
				contentColor = JellyfinTheme.colorScheme.onButtonActive,
			)
		} else {
			ButtonDefaults.colors()
		},
	) {
		Icon(
			imageVector = ImageVector.vectorResource(R.drawable.ic_users),
			contentDescription = if (group != null) {
				stringResource(R.string.syncplay_active, group?.groupName.orEmpty())
			} else {
				stringResource(R.string.syncplay)
			},
		)
	}
}
