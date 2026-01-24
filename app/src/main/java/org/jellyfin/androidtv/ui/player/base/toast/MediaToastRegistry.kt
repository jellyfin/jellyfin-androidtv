package org.jellyfin.androidtv.ui.player.base.toast

import androidx.annotation.DrawableRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MediaToastRegistry(
	private val coroutineScope: CoroutineScope,
) {
	companion object {
		val TOAST_DURATION = 700.milliseconds
	}

	private val _current = MutableStateFlow<MediaToastData?>(null)
	val current = _current.asStateFlow()

	private var unsetJob: Job? = null

	fun emit(data: MediaToastData) {
		if (current.value == data) return

		_current.value = data

		unsetJob?.cancel()
		unsetJob = coroutineScope.launch {
			delay(TOAST_DURATION)
			_current.value = null
		}
	}

	fun emit(
		@DrawableRes icon: Int,
		progress: Float? = null,
	) {
		val data = MediaToastData(
			icon = icon,
			progress = progress,
		)
		emit(data)
	}
}

