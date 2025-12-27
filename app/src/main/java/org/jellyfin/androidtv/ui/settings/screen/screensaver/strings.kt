package org.jellyfin.androidtv.ui.settings.screen.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.getQuantityString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
@Stable
fun getScreensaverAgeRatingOptions() = buildList {
	add(0 to stringResource(R.string.pref_screensaver_ageratingmax_zero))
	setOf(5, 10, 13, 14, 16, 18, 21)
		.forEach { age -> add(age to stringResource(R.string.pref_screensaver_ageratingmax_entry, age)) }
	add(-1 to stringResource(R.string.pref_screensaver_ageratingmax_unlimited))
}

@Composable
@Stable
fun getScreensaverTimeoutOptions() = buildList {
	val context = LocalContext.current

	add(30.seconds to context.getQuantityString(R.plurals.seconds, 30))
	add(1.minutes to context.getQuantityString(R.plurals.minutes, 1))
	add(2.5.minutes to context.getQuantityString(R.plurals.minutes, 2.5))
	add(5.minutes to context.getQuantityString(R.plurals.minutes, 5))
	add(10.minutes to context.getQuantityString(R.plurals.minutes, 10))
	add(15.minutes to context.getQuantityString(R.plurals.minutes, 15))
	add(30.minutes to context.getQuantityString(R.plurals.minutes, 30))
}
