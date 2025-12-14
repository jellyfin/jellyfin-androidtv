package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsAuthenticationServerUserScreen(serverId: UUID, userId: UUID) {
	val router = LocalRouter.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val user = remember(server) { server?.let(serverUserRepository::getStoredServerUsers)?.find { user -> user.id == userId } }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(server?.name?.uppercase().orEmpty()) },
				headingContent = { Text(user?.name.orEmpty()) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_sign_out)) },
				captionContent = { Text(stringResource(R.string.lbl_sign_out_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) authenticationRepository.logout(user)
						router.back()
					}
				}
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.lbl_remove)) },
				captionContent = { Text(stringResource(R.string.lbl_remove_user_content)) },
				onClick = {
					lifecycleScope.launch {
						if (user != null) serverUserRepository.deleteStoredUser(user)
						router.back()
					}
				}
			)
		}
	}
}
