package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R

@Composable
@Suppress("MagicNumber")
fun getResolutionName(width: Int, height: Int, interlaced: Boolean = false): String {
	val suffix = if (interlaced) "i" else "p"
	return when {
		width >= 7600 || height >= 4300 -> "8K"
		width >= 3800 || height >= 2000 -> "4K"
		width >= 2500 || height >= 1400 -> "1440$suffix"
		width >= 1800 || height >= 1000 -> "1080$suffix"
		width >= 1200 || height >= 700 -> "720$suffix"
		width >= 600 || height >= 400 -> "480$suffix"

		else -> stringResource(R.string.lbl_sd)
	}
}
