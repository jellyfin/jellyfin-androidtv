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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

@Composable
fun SettingsAuthenticationAutoSignInScreen() {
	val router = LocalRouter.current
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	var autoLoginUserBehavior by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserBehavior)
	var autoLoginServerId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginServerId)
	var autoLoginUserId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserId)

	LaunchedEffect(serverRepository) {
		serverRepository.loadStoredServers()
	}

	val storedServers by serverRepository.storedServers.collectAsState()

	LazyColumn(
		modifier = Modifier
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.auto_sign_in)) },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.user_picker_disable_title)) },
				captionContent = { Text(stringResource(R.string.user_picker_disable_summary)) },
				trailingContent = { RadioButton(checked = autoLoginUserBehavior == UserSelectBehavior.DISABLED) },
				onClick = {
					autoLoginUserBehavior = UserSelectBehavior.DISABLED
					router.back()
				}
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.user_picker_last_user_title)) },
				captionContent = { Text(stringResource(R.string.user_picker_last_user_summary)) },
				trailingContent = { RadioButton(checked = autoLoginUserBehavior == UserSelectBehavior.LAST_USER) },
				onClick = {
					autoLoginUserBehavior = UserSelectBehavior.LAST_USER
					router.back()
				}
			)
		}

		for (server in storedServers) {
			item {
				ListSection(
					headingContent = { Text(server.name) },
				)
			}

			val users = serverUserRepository.getStoredServerUsers(server)
			val serverId = server.id.toString()
			items(users) { user ->
				val userId = user.id.toString()
				val userImagePainter = rememberAsyncImagePainter(authenticationRepository.getUserImageUrl(server, user))
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
					trailingContent = { RadioButton(checked = autoLoginUserBehavior == UserSelectBehavior.SPECIFIC_USER && autoLoginServerId == serverId && autoLoginUserId == userId) },
					onClick = {
						autoLoginUserBehavior = UserSelectBehavior.SPECIFIC_USER
						autoLoginServerId = serverId
						autoLoginUserId = userId

						router.back()
					}
				)
			}
		}
	}
}
