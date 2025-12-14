package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun SettingsAuthenticationServerScreen(serverId: UUID) {
	val router = LocalRouter.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val users = remember(server) { server?.let(serverUserRepository::getStoredServerUsers) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(server?.name.orEmpty()) },
				captionContent = { Text(server?.address.orEmpty()) },
			)
		}

		if (!users.isNullOrEmpty()) {
			item { ListSection(headingContent = { Text(stringResource(R.string.pref_accounts)) }) }

			items(users) { user ->
				ListButton(
					leadingContent = {
						ProfilePicture(
							url = authenticationRepository.getUserImageUrl(requireNotNull(server), user),
							modifier = Modifier
								.size(24.dp)
								.clip(IconButtonDefaults.Shape)
						)
					},
					headingContent = { Text(user.name) },
					captionContent = {
						val lastUsedDate = LocalDateTime.ofInstant(
							Instant.ofEpochMilli(user.lastUsed),
							ZoneId.systemDefault()
						)
						Text(
							stringResource(
								R.string.lbl_user_last_used,
								lastUsedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
								lastUsedDate.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
							)
						)
					},
					onClick = {
						router.push(
							route = Routes.AUTHENTICATION_SERVER_USER,
							parameters = mapOf(
								"serverId" to user.serverId.toString(),
								"userId" to user.id.toString(),
							),
						)
					}
				)
			}
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.lbl_server)) }) }

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_remove_server)) },
				captionContent = { Text(stringResource(R.string.lbl_remove_users)) },
				onClick = {
					lifecycleScope.launch {
						serverRepository.deleteServer(server?.id ?: serverId)
						router.back()
					}
				}
			)
		}
	}
}
