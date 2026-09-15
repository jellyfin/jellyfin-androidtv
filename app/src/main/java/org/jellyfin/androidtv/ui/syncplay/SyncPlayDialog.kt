package org.jellyfin.androidtv.ui.syncplay

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.CircularProgressIndicator
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayFailure
import org.jellyfin.sdk.model.api.GroupStateType
import org.jellyfin.sdk.model.api.SyncPlayUserAccessType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod") // One declarative TV menu with mutually exclusive membership states.
fun SyncPlayDialog() {
	val viewModel = koinActivityViewModel<SyncPlayViewModel>()
	val userRepository = koinInject<UserRepository>()
	val uiState by viewModel.uiState.collectAsState()
	val syncState by viewModel.syncPlayState.collectAsState()
	val user by userRepository.currentUser.collectAsState()
	val access = user?.policy?.syncPlayAccess ?: SyncPlayUserAccessType.NONE

	SettingsDialog(
		visible = uiState.visible && access != SyncPlayUserAccessType.NONE,
		onDismissRequest = viewModel::dismiss,
	) {
		SettingsColumn {
			item {
				ListSection(
					overlineContent = { Text(stringResource(R.string.syncplay)) },
					headingContent = {
						Text(syncState.group?.groupName ?: stringResource(R.string.syncplay_watch_together))
					},
					captionContent = {
						Text(
							when {
								syncState.group != null -> syncState.group?.participants.orEmpty().joinToString()
								else -> stringResource(R.string.syncplay_description)
							}
						)
					},
				)
			}

			val error = uiState.error?.let {
				when (it) {
					SyncPlayUiError.GROUPS -> R.string.syncplay_error_groups
					SyncPlayUiError.MEMBERSHIP -> R.string.syncplay_error_membership
					SyncPlayUiError.ACTION -> R.string.syncplay_error_action
				}
			} ?: syncState.error?.let {
				when (it) {
					SyncPlayFailure.REQUEST -> R.string.syncplay_error_action
					SyncPlayFailure.DISCONNECTED -> R.string.syncplay_error_disconnected
					SyncPlayFailure.PLAYBACK -> R.string.syncplay_error_playback
					SyncPlayFailure.GROUP_MISSING -> R.string.syncplay_error_group_missing
					SyncPlayFailure.ACCESS_DENIED -> R.string.syncplay_error_access_denied
				}
			}
			if (error != null) item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_error), contentDescription = null) },
					headingContent = { Text(stringResource(error)) },
					captionContent = { Text(stringResource(R.string.syncplay_retry)) },
					onClick = viewModel::refresh,
					enabled = !uiState.loading,
				)
			}

			if (uiState.loading || syncState.joining) item {
				ListSection(
					leadingContent = { CircularProgressIndicator(Modifier.size(20.dp)) },
					headingContent = { Text(stringResource(R.string.loading)) },
				)
			}

			if (syncState.group == null) {
				if (!uiState.loading && uiState.groups.isEmpty() && error == null) item {
					ListSection(headingContent = { Text(stringResource(R.string.syncplay_no_groups)) })
				}
				items(uiState.groups.size) { index ->
					val group = uiState.groups[index]
					ListButton(
						leadingContent = { Icon(painterResource(R.drawable.ic_users), contentDescription = null) },
						headingContent = { Text(group.groupName) },
						captionContent = {
							Text(group.participants.joinToString().ifEmpty { stringResource(R.string.syncplay_empty_group) })
						},
						onClick = { viewModel.join(group.groupId) },
						enabled = !uiState.loading,
					)
				}
				if (access == SyncPlayUserAccessType.CREATE_AND_JOIN_GROUPS) item {
					val name = stringResource(R.string.syncplay_default_group_name, user?.name.orEmpty())
					ListButton(
						leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
						headingContent = { Text(stringResource(R.string.syncplay_create_group)) },
						onClick = { viewModel.create(name) },
						enabled = !uiState.loading,
					)
				}
			} else {
				item {
					val stateLabel = when (syncState.group?.state) {
						GroupStateType.PLAYING -> R.string.syncplay_state_playing
						GroupStateType.PAUSED -> R.string.syncplay_state_paused
						GroupStateType.WAITING -> R.string.syncplay_state_waiting
						else -> R.string.syncplay_state_idle
					}
					ListSection(headingContent = { Text(stringResource(stateLabel)) })
				}
				item {
					ListButton(
						leadingContent = {
							Icon(painterResource(if (syncState.following) R.drawable.ic_pause else R.drawable.ic_play), null)
						},
						headingContent = {
							Text(stringResource(if (syncState.following) R.string.syncplay_stop_following else R.string.syncplay_resume_following))
						},
						onClick = { if (syncState.following) viewModel.halt() else viewModel.resume() },
						enabled = !uiState.loading,
					)
				}
				item {
					ListButton(
						leadingContent = { Icon(painterResource(R.drawable.ic_users), contentDescription = null) },
						headingContent = { Text(stringResource(R.string.syncplay_leave_group)) },
						onClick = viewModel::leave,
						enabled = !uiState.loading,
					)
				}
			}
		}
	}
}
