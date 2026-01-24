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
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.preference.repository.UserPinRepository
import org.jellyfin.androidtv.ui.startup.PinDialogFragment
import java.util.UUID

@Composable
fun SettingsAuthenticationServerUserScreen(serverId: UUID, userId: UUID) {
	val router = LocalRouter.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val lifecycleScope = lifecycleOwner.lifecycleScope
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationRepository = koinInject<AuthenticationRepository>()
	val userPinRepository = koinInject<UserPinRepository>()

	val context = LocalContext.current
	val fragmentManager = remember(context) {
		generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
			.filterIsInstance<FragmentActivity>()
			.firstOrNull()?.supportFragmentManager
	}

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val server by remember(serverRepository.storedServers) {
		serverRepository.storedServers.map { it.find { server -> server.id == serverId } }
	}.collectAsState(null)

	val user = remember(server) { server?.let(serverUserRepository::getStoredServerUsers)?.find { user -> user.id == userId } }
	
	var hasPin by remember(userId) { mutableStateOf(userPinRepository.hasPin(userId)) }

	DisposableEffect(lifecycleOwner, fragmentManager) {
		if (fragmentManager != null) {
			// Result listener for Setting a PIN (Initial set or Change after verification)
			fragmentManager.setFragmentResultListener("pin_set_request", lifecycleOwner) { _, _ ->
				hasPin = userPinRepository.hasPin(userId)
			}

			// Result listener for Verifying before Changing
			fragmentManager.setFragmentResultListener("pin_verify_change_request", lifecycleOwner) { _, result ->
				if (result.getBoolean(PinDialogFragment.RESULT_EXTRA_SUCCESS)) {
					// Verification successful, now show SET dialog
					PinDialogFragment.newInstance(userId, "pin_set_request", PinDialogFragment.Companion.Mode.SET).show(fragmentManager, "pin_dialog_set")
				}
			}

			// Result listener for Verifying before Removing
			fragmentManager.setFragmentResultListener("pin_verify_remove_request", lifecycleOwner) { _, result ->
				if (result.getBoolean(PinDialogFragment.RESULT_EXTRA_SUCCESS)) {
					// Verification successful, remove PIN
					userPinRepository.removePin(userId)
					hasPin = false
				}
			}
		}
		onDispose {
			fragmentManager?.clearFragmentResultListener("pin_set_request")
			fragmentManager?.clearFragmentResultListener("pin_verify_change_request")
			fragmentManager?.clearFragmentResultListener("pin_verify_remove_request")
		}
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(server?.name?.uppercase().orEmpty()) },
				headingContent = { Text(user?.name.orEmpty()) },
			)
		}

		item {
			if (hasPin) {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock_outline), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.lbl_change_pin)) },
					onClick = {
						fragmentManager?.let {
							// Verify first
							PinDialogFragment.newInstance(userId, "pin_verify_change_request", PinDialogFragment.Companion.Mode.VERIFY).show(it, "pin_dialog_verify_change")
						}
					}
				)
			} else {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock_outline), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.lbl_set_pin)) },
					onClick = {
						fragmentManager?.let {
							PinDialogFragment.newInstance(userId, "pin_set_request", PinDialogFragment.Companion.Mode.SET).show(it, "pin_dialog_set")
						}
					}
				)
			}
		}

		if (hasPin) {
			item {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_lock_open_outline), contentDescription = null) },
					headingContent = { Text(stringResource(R.string.lbl_remove_pin)) },
					onClick = {
						fragmentManager?.let {
							// Verify first
							PinDialogFragment.newInstance(userId, "pin_verify_remove_request", PinDialogFragment.Companion.Mode.VERIFY).show(it, "pin_dialog_verify_remove")
						}
					}
				)
			}
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
