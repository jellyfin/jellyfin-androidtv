package org.jellyfin.androidtv.ui.preference

import androidx.compose.foundation.LocalIndication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDrawer(
	modifier: Modifier = Modifier,
	drawerState: DrawerState = rememberDrawerState(DrawerValue.Open),
	content: @Composable () -> Unit,
) {
	// Reverse layout direction so that the drawer opens from the right side
	val layoutDirectionDefault = LocalLayoutDirection.current
	val layoutDirectionReversed = when (layoutDirectionDefault) {
		LayoutDirection.Ltr -> LayoutDirection.Rtl
		LayoutDirection.Rtl -> LayoutDirection.Ltr
	}
	val rippleIndication = rememberRipple(color = Color.White)
	CompositionLocalProvider(
		LocalLayoutDirection provides layoutDirectionReversed,
		LocalIndication provides rippleIndication,
	) {
		ModalNavigationDrawer(
			modifier = modifier,
			drawerState = drawerState,
			drawerContent = {
				CompositionLocalProvider(
					LocalLayoutDirection provides layoutDirectionDefault,
				) {
					content()
				}
			},
			content = {
				// No content
			},
		)
	}
}
