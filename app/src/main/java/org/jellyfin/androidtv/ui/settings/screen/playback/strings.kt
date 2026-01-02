package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.TimeUtils

@Composable
@Stable
fun getResumeSubtractDurationOptions(): Map<String, String> {
	val context = LocalContext.current
	return setOf(
		0, // Disable
		3, 5, 7, // 10<
		10, 20, 30, 60, // 100<
		120, 300
	).associate {
		val value = if (it == 0) stringResource(R.string.lbl_none)
		else TimeUtils.formatSeconds(context, it)

		it.toString() to value
	}
}
