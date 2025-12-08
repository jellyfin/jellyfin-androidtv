package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
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

	LaunchedEffect(serverRepository) {
		serverRepository.loadStoredServers()
	}

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val users = remember(server) {
		server?.let(serverUserRepository::getStoredServerUsers)
	}

	LazyColumn(
		modifier = Modifier
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(server?.name.orEmpty()) },
				captionContent = { Text(server?.address.orEmpty()) },
			)
		}

		if (!users.isNullOrEmpty()) {
			item {
				ListSection(
					modifier = Modifier,
					headingContent = { Text(stringResource(R.string.pref_accounts)) },
				)
			}

			items(users) { user ->
				val userImagePainter = rememberAsyncImagePainter(authenticationRepository.getUserImageUrl(server!!, user))
				val userImageState by userImagePainter.state.collectAsState()
				val userImageVisible = userImageState is AsyncImagePainter.State.Success

				ListButton(
					leadingContent = {
						if (!userImageVisible) {
							Icon(
								imageVector = ImageVector.vectorResource(R.drawable.ic_user),
								contentDescription = null,
							)
						} else {
							Image(
								painter = userImagePainter,
								contentDescription = null,
								contentScale = ContentScale.Crop,
								modifier = Modifier
									.width(24.dp)
									.aspectRatio(1f)
									.clip(IconButtonDefaults.Shape)
							)
						}
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

		item {
			ListSection(
				modifier = Modifier,
				headingContent = { Text(stringResource(R.string.lbl_server)) },
			)
		}

		item {
			ListButton(
				leadingContent = {
					Icon(
						painterResource(R.drawable.ic_delete),
						contentDescription = stringResource(R.string.lbl_remove_server)
					)
				},
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
