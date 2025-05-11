package org.jellyfin.androidtv.ui.composable

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.util.locale
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun rememberCurrentTime(
	format12: String = "h:mm",
	format24: String = "k:mm",
	updateFrequency: Duration = 1.minutes,
): State<String> {
	val context = LocalContext.current
	val locale = context.locale
	val format = remember(context, locale) {
		when (DateFormat.is24HourFormat(context)) {
			true -> format24
			false -> format12
		}
	}
	val currentTime = remember { mutableStateOf(DateFormat.format(format, Date()).toString()) }

	LaunchedEffect(format) {
		while (true) {
			val now = Date()
			currentTime.value = DateFormat.format(format, now).toString()
			// Delay until next expected change in time based on updateFrequency
			delay(updateFrequency.inWholeMilliseconds - now.time % updateFrequency.inWholeMilliseconds)
		}
	}

	return currentTime
}
