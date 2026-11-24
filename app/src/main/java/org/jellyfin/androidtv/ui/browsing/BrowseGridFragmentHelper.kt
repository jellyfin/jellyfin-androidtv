package org.jellyfin.androidtv.ui.browsing

import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.leanback.widget.BaseGridView
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import java.util.UUID

fun BaseGridView.moveGridFocus(event: KeyEvent): Boolean {
	if (event.action != KeyEvent.ACTION_DOWN) return false

	val direction = when (event.keyCode) {
		KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
		KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
		KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
		KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT

		else -> return false
	}

	val focused = findFocus()
		?.takeIf { it != this } ?: return false

	val next = focusSearch(focused, direction) ?: return false
	return next == focused || next.requestFocus(direction)
}

fun BrowseGridFragment.createSettingsVisibility() = MutableStateFlow(false)

fun BrowseGridFragment.addSettings(
	view: ComposeView,
	itemId: UUID,
	displayPreferencesId: String,
	visible: MutableStateFlow<Boolean>,
) {
	view.setContent {
		val isVisible by visible.collectAsState(false)

		ProvideRouter(
			routes,
			Routes.LIBRARIES_DISPLAY,
			mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
		) {
			SettingsDialog(
				visible = isVisible,
				onDismissRequest = {
					visible.value = false
					onResume()
				}
			) {
				SettingsRouterContent()
			}
		}
	}
}
