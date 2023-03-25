package org.jellyfin.androidtv.ui.preference

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
	modifier: Modifier = Modifier,
	onClose: () -> Unit,
) {
	MaterialTheme(
		colorScheme = darkColorScheme(),
	) {
		val coroutineScope = rememberCoroutineScope()
		val drawerState = rememberDrawerState(DrawerValue.Closed)

		// Open on launch for a nice animation
		LaunchedEffect(Unit) {
			coroutineScope.launch {
				drawerState.open()
			}
		}

		// Invoke the close lambda when the drawer is closing
		LaunchedEffect(drawerState.targetValue) {
			if (drawerState.isOpen && drawerState.targetValue == DrawerValue.Closed) {
				onClose()
			}
		}

		PreferencesDrawer(
			modifier = modifier,
			drawerState = drawerState,
		) {
			ModalDrawerSheet(
				drawerShape = RectangleShape,
				drawerContainerColor = Color(
					ContextCompat.getColor(
						LocalContext.current,
						R.color.default_preference_window_background
					)
				),
			) {
				BackHandler(drawerState.isOpen) {
					coroutineScope.launch {
						drawerState.close()
					}
				}

				MainPreferencesScreen()
			}
		}
	}
}
