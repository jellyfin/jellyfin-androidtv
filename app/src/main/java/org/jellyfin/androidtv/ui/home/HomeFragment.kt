package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val homeViewModel by activityViewModel<HomeViewModel>()
	private val userPreferences by inject<UserPreferences>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val rowsFocusRequester = remember { FocusRequester() }
		LaunchedEffect(rowsFocusRequester) { rowsFocusRequester.requestFocus() }

		// Observe preference changes
		var enhancedHomeEnabled by remember { mutableStateOf(userPreferences[UserPreferences.enhancedHomeScreen]) }

		DisposableEffect(Unit) {
			val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				if (key == UserPreferences.enhancedHomeScreen.key) {
					enhancedHomeEnabled = userPreferences[UserPreferences.enhancedHomeScreen]
					// Update background service based on preference
					if (enhancedHomeEnabled) {
						backgroundService.disable()
					} else {
						backgroundService.clearBackgrounds()
					}
				}
			}

			// Access SharedPreferences directly from the preference store
			val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
			sharedPrefs.registerOnSharedPreferenceChangeListener(listener)

			onDispose {
				sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
			}
		}

		if (enhancedHomeEnabled) {
			// Enhanced home screen with hero section
			Box(modifier = Modifier.fillMaxSize()) {
				// Hero backdrop - extends down behind the rows
				HomeHeroSection(
					viewModel = homeViewModel,
					modifier = Modifier.fillMaxSize()
				)

				// Foreground content
				Column(modifier = Modifier.fillMaxSize()) {
					MainToolbar(MainToolbarActiveButton.Home)

					// Spacer to position rows over lower part of backdrop
					Spacer(modifier = Modifier.height(160.dp))

					// The leanback code has its own awful focus handling that doesn't work properly with Compose view inteop to workaround this
					// issue we add custom behavior that only allows focus exit when the current selected row is the first one. Additionally when
					// we do switch the focus, we reset the leanback state so it won't cause weird behavior when focus is regained
					var rowsSupportFragment by remember { mutableStateOf<HomeRowsFragment?>(null) }
					AndroidFragment<HomeRowsFragment>(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f) // Take remaining space after toolbar and spacer
							.focusGroup()
							.focusRequester(rowsFocusRequester)
							.focusProperties {
								onExit = {
									val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
									if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
										cancelFocusChange()
									} else {
										rowsSupportFragment?.selectedPosition = 0
										rowsSupportFragment?.verticalGridView?.clearFocus()
									}
								}
							},
						onUpdate = { fragment ->
							rowsSupportFragment = fragment
						}
					)
				}
			}
		} else {
			// Original home screen without hero section
			Column(modifier = Modifier.fillMaxSize()) {
				MainToolbar(MainToolbarActiveButton.Home)

				var rowsSupportFragment by remember { mutableStateOf<HomeRowsFragment?>(null) }
				AndroidFragment<HomeRowsFragment>(
					modifier = Modifier
						.focusGroup()
						.focusRequester(rowsFocusRequester)
						.focusProperties {
							onExit = {
								val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
								if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
									cancelFocusChange()
								} else {
									rowsSupportFragment?.selectedPosition = 0
									rowsSupportFragment?.verticalGridView?.clearFocus()
								}
							}
						}
						.fillMaxSize(),
					onUpdate = { fragment ->
						rowsSupportFragment = fragment
					}
				)
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		// Re-enable background service when leaving home screen
		backgroundService.clearBackgrounds()
	}
}
