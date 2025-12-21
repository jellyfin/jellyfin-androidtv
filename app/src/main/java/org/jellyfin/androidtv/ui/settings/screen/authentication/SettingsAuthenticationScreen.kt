package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.compose.koinInject

@Composable
fun SettingsAuthenticationScreen(launchedFromLogin: Boolean = false) {
	val router = LocalRouter.current
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationPreferences = koinInject<AuthenticationPreferences>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val storedServers by serverRepository.storedServers.collectAsState()

	SettingsColumn {
		if (launchedFromLogin) item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_login)) },
			)
		} else item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_login)) },
			)
		}

		item {
			var autoLoginUserBehavior by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserBehavior)
			var autoLoginServerId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginServerId)
			var autoLoginUserId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserId)
			val autoLoginServer = remember(storedServers, autoLoginServerId) {
				storedServers.find { server -> server.id == autoLoginServerId.toUUIDOrNull() }
			}
			val autoLoginUser = remember(autoLoginServer, autoLoginUserId) {
				val users = autoLoginServer?.let(serverUserRepository::getStoredServerUsers)
				users?.find { user -> user.id == autoLoginUserId.toUUIDOrNull() }
			}

			ListButton(
				headingContent = { Text(stringResource(R.string.auto_sign_in)) },
				captionContent = {
					when (autoLoginUserBehavior) {
						UserSelectBehavior.DISABLED -> Text(stringResource(R.string.user_picker_disable_title))
						UserSelectBehavior.LAST_USER -> Text(stringResource(R.string.user_picker_last_user_title))
						UserSelectBehavior.SPECIFIC_USER -> Text(autoLoginUser?.name ?: stringResource(R.string.loading))
					}
				},
				onClick = { router.push(Routes.AUTHENTICATION_AUTO_SIGN_IN) }
			)
		}

		item {
			var sortBy by rememberPreference(authenticationPreferences, AuthenticationPreferences.sortBy)
			ListButton(
				headingContent = { Text(stringResource(R.string.sort_accounts_by)) },
				captionContent = { Text(stringResource(sortBy.nameRes)) },
				onClick = { router.push(Routes.AUTHENTICATION_SORT_BY) }
			)
		}

		if (storedServers.isNotEmpty()) {
			item { ListSection(headingContent = { Text(stringResource(R.string.lbl_manage_servers)) }) }

			items(storedServers) { server ->
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
					headingContent = { Text(server.name) },
					captionContent = { Text(server.address) },
					onClick = {
						router.push(
							route = Routes.AUTHENTICATION_SERVER,
							parameters = mapOf(
								"serverId" to server.id.toString(),
							),
						)
					}
				)
			}
		}

		// Disallow changing the "always authenticate" option from the login screen
		// because that could allow a kid to disable the function to access a parent's account
		if (!launchedFromLogin) {
			item { ListSection(headingContent = { Text(stringResource(R.string.advanced_settings)) }) }

			item {
				var alwaysAuthenticate by rememberPreference(authenticationPreferences, AuthenticationPreferences.alwaysAuthenticate)
				ListButton(
					headingContent = { Text(stringResource(R.string.always_authenticate)) },
					trailingContent = { Checkbox(checked = alwaysAuthenticate) },
					captionContent = { Text(stringResource(R.string.always_authenticate_description)) },
					onClick = { alwaysAuthenticate = !alwaysAuthenticate }
				)
			}
		}

		if (launchedFromLogin) item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_jellyfin), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
				onClick = { router.push(Routes.ABOUT, mapOf("fromLogin" to "true")) }
			)
		}
	}
}
