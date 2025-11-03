package org.jellyfin.androidtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject

private typealias Lock = () -> Unit

@Composable
fun ScreensaverLock(
	enabled: Boolean
) {
	val viewModel = koinInject<InteractionTrackerViewModel>()
	val lifecycleOwner = LocalLifecycleOwner.current

	var lock by remember { mutableStateOf<Lock?>(null) }

	DisposableEffect(enabled, lifecycleOwner) {
		if (enabled && lock == null) {
			lock = viewModel.addLifecycleLock(lifecycleOwner.lifecycle)
		}

		onDispose {
			lock?.invoke()
			lock = null
		}
	}
}
