package org.jellyfin.androidtv.ui.syncplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayService
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.koin.compose.koinInject

@Composable
fun SyncPlayDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	playbackManager: PlaybackManager = koinInject(),
) {
	val service = playbackManager.getService<SyncPlayService>() ?: return
	LaunchedEffect(service, visible) {
		if (visible) {
			service.clearError()
			service.refreshGroups()
		}
	}

	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
	) {
		ProvideTextStyle(LocalTextStyle.current.copy(color = JellyfinTheme.colorScheme.onBackground)) {
			SyncPlayDialogContent(service, onDismissRequest)
		}
	}
}

@Composable
private fun SyncPlayDialogContent(
	service: SyncPlayService,
	onDismissRequest: () -> Unit,
) {
	val group by service.group.collectAsState()
	val busy by service.busy.collectAsState()
	val scope = rememberCoroutineScope()
	var creatingGroup by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }
	val scrollState = rememberScrollState()

	Column(
		verticalArrangement = Arrangement.spacedBy(16.dp),
		modifier = Modifier
			.width(560.dp)
			.heightIn(max = 480.dp)
			.background(JellyfinTheme.colorScheme.surface, JellyfinTheme.shapes.large)
			.padding(24.dp)
			.verticalScroll(scrollState)
			.focusRequester(focusRequester)
			.focusGroup(),
	) {
		Text(stringResource(R.string.syncplay), fontSize = 24.sp, fontWeight = FontWeight.Bold)
		Text(stringResource(R.string.syncplay_description))

		SyncPlayStatus(service, busy)

		val currentGroup = group
		when {
			currentGroup != null -> CurrentGroup(
				group = currentGroup,
				busy = busy,
				onLeave = { scope.launch { service.leaveGroup() } },
			)

			creatingGroup -> CreateGroupForm(
				busy = busy,
				onCreate = { name -> scope.launch { service.createGroup(name) } },
				onCancel = {
					creatingGroup = false
					service.clearError()
				},
			)

			else -> AvailableGroups(
				service = service,
				busy = busy,
				onCreate = {
					service.clearError()
					creatingGroup = true
				},
			)
		}

		DriftCorrectionOption(service)

		Button(onClick = onDismissRequest) {
			Text(stringResource(R.string.syncplay_close))
		}
	}

	LaunchedEffect(group?.groupId, creatingGroup, busy) {
		// Joining, leaving, and changing forms can remove the focused control.
		if (!busy) {
			scrollState.scrollTo(0)
			focusRequester.requestFocus()
		}
		if (group != null) creatingGroup = false
	}
}

@Composable
private fun SyncPlayStatus(service: SyncPlayService, busy: Boolean) {
	val error by service.error.collectAsState()
	if (error != null) {
		Text(stringResource(R.string.syncplay_error, error.orEmpty()))
		Button(onClick = service::clearError) {
			Text(stringResource(R.string.syncplay_dismiss_error))
		}
	}
	if (busy) Text(stringResource(R.string.loading))
}

@Composable
private fun CurrentGroup(
	group: GroupInfoDto,
	busy: Boolean,
	onLeave: () -> Unit,
) {
	Text(stringResource(R.string.syncplay_current_group, group.groupName), fontWeight = FontWeight.Bold)
	GroupParticipants(group)
	Text(stringResource(R.string.syncplay_group_hint))
	Button(onClick = onLeave, enabled = !busy) {
		Text(stringResource(R.string.syncplay_leave_group))
	}
}

@Composable
private fun AvailableGroups(
	service: SyncPlayService,
	busy: Boolean,
	onCreate: () -> Unit,
) {
	val groups by service.groups.collectAsState()
	val scope = rememberCoroutineScope()
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		Button(
			onClick = { scope.launch { service.refreshGroups() } },
			enabled = !busy,
		) {
			Text(stringResource(R.string.syncplay_refresh))
		}
		Button(onClick = onCreate, enabled = !busy) {
			Text(stringResource(R.string.syncplay_create_group))
		}
	}
	Text(stringResource(R.string.syncplay_available_groups), fontWeight = FontWeight.Bold)
	if (groups.isEmpty() && !busy) Text(stringResource(R.string.syncplay_no_groups))
	for (group in groups) {
		ListButton(
			onClick = { scope.launch { service.joinGroup(group.groupId) } },
			enabled = !busy,
			headingContent = { Text(stringResource(R.string.syncplay_join_group, group.groupName)) },
			captionContent = { GroupParticipants(group) },
		)
	}
}

@Composable
private fun DriftCorrectionOption(service: SyncPlayService) {
	val enabled by service.syncCorrectionEnabled.collectAsState()
	val interactionSource = remember { MutableInteractionSource() }
	ListControl(
		modifier = Modifier.toggleable(
			value = enabled,
			interactionSource = interactionSource,
			indication = null,
			role = Role.Checkbox,
			onValueChange = service::setSyncCorrectionEnabled,
		),
		interactionSource = interactionSource,
		headingContent = { Text(stringResource(R.string.syncplay_correct_drift)) },
		captionContent = { Text(stringResource(R.string.syncplay_correct_drift_description)) },
		trailingContent = { Checkbox(checked = enabled) },
	)
}

@Composable
private fun GroupParticipants(group: GroupInfoDto) {
	val participants = group.participants
	if (participants.isNotEmpty()) {
		Text(stringResource(R.string.syncplay_participants, participants.joinToString()))
	}
}

@Composable
private fun CreateGroupForm(
	busy: Boolean,
	onCreate: (String) -> Unit,
	onCancel: () -> Unit,
) {
	var name by remember { mutableStateOf("") }
	val keyboardController = LocalSoftwareKeyboardController.current
	val canCreate = !busy && name.isNotBlank()
	val create = {
		if (canCreate) {
			keyboardController?.hide()
			onCreate(name.trim())
		}
	}

	GroupNameField(name, { name = it }, !busy, create)
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		Button(onClick = create, enabled = canCreate) {
			Text(stringResource(R.string.syncplay_create_group))
		}
		Button(onClick = onCancel, enabled = !busy) {
			Text(stringResource(R.string.lbl_cancel))
		}
	}
}

@Composable
private fun GroupNameField(name: String, onNameChange: (String) -> Unit, enabled: Boolean, onDone: () -> Unit) {
	val keyboardController = LocalSoftwareKeyboardController.current
	val focusManager = LocalFocusManager.current
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val label = stringResource(R.string.syncplay_group_name)
	Text(label, fontWeight = FontWeight.Bold)
	BasicTextField(
		value = name,
		onValueChange = onNameChange,
		singleLine = true,
		enabled = enabled,
		interactionSource = interactionSource,
		textStyle = LocalTextStyle.current,
		cursorBrush = SolidColor(JellyfinTheme.colorScheme.onInput),
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
		keyboardActions = KeyboardActions(onDone = { onDone() }),
		modifier = Modifier
			.fillMaxWidth()
			.onPreviewKeyEvent {
				if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
					keyboardController?.hide()
					focusManager.moveFocus(FocusDirection.Next)
				} else {
					false
				}
			}
			.semantics { contentDescription = label },
		decorationBox = { innerTextField ->
			Box(
				modifier = Modifier
					.border(
						width = 2.dp,
						color = if (focused) JellyfinTheme.colorScheme.inputFocused else JellyfinTheme.colorScheme.input,
						shape = JellyfinTheme.shapes.small,
					)
					.padding(12.dp),
			) {
				innerTextField()
			}
		},
	)
}
