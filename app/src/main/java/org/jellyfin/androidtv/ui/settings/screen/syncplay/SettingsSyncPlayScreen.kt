package org.jellyfin.androidtv.ui.settings.screen.syncplay

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.syncplay.SyncPlayViewModel
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsSyncPlayScreen() {
	val viewModel = koinViewModel<SyncPlayViewModel>()
	val state by viewModel.state.collectAsState()
	val defaultGroupName = stringResource(R.string.syncplay_default_group_name)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.syncplay_title)) },
				captionContent = { Text(stringResource(R.string.syncplay_description)) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_loop), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.syncplay_refresh_groups)) },
				onClick = { viewModel.refreshGroups() },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.syncplay_create_group)) },
				onClick = { viewModel.createGroup(defaultGroupName) },
			)
		}

		if (state.activeGroup != null) {
			item {
				ListSection(headingContent = { Text(stringResource(R.string.syncplay_active_group)) })
			}
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_play), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.syncplay_sync_current)) },
					captionContent = { Text(state.activeGroup?.groupName.orEmpty()) },
					onClick = { viewModel.syncCurrentPlayback() },
				)
			}
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.syncplay_leave_group)) },
					onClick = { viewModel.leaveGroup() },
				)
			}
		}

		item {
			ListSection(headingContent = { Text(stringResource(R.string.syncplay_available_groups)) })
		}

		if (state.groups.isEmpty()) {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_help), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.syncplay_no_groups)) },
					onClick = { viewModel.refreshGroups() },
				)
			}
		} else {
			items(state.groups) { group ->
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_users), contentDescription = null) },
					headingContent = { Text(group.groupName) },
					captionContent = { Text(group.groupId.toString()) },
					onClick = { viewModel.joinGroup(group.groupId) },
				)
			}
		}
	}
}
