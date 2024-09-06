package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jellyfin.androidtv.integration.dream.DreamViewModel
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun DreamHost() {
	val viewModel = koinViewModel<DreamViewModel>()
	val userPreferences = koinInject<UserPreferences>()
	val content by viewModel.content.collectAsState()

	DreamView(
		content = content,
		showClock = when (userPreferences[UserPreferences.clockBehavior]) {
			ClockBehavior.ALWAYS, ClockBehavior.IN_MENUS -> true
			else -> false
		}
	)
}
