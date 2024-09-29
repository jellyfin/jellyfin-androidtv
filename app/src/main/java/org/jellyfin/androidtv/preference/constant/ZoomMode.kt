package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum
import timber.log.Timber

enum class ZoomMode(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Sets the zoom mode to Normal (Fit).
	 */
	ZOOM_FIT(R.string.lbl_fit),

	/**
	 * Sets the zoom mode to auto crop.
	 */
	AUTO_CROP(R.string.lbl_auto_crop),

	/**
	 * Sets the zoom mode to stretch.
	 */
	STRETCH(R.string.lbl_stretch),
}

const val ZOOM_FIT: Int = 0;
const val ZOOM_AUTO_CROP: Int = 1;
const val ZOOM_STRETCH: Int = 2;

fun getZoomModeFromId(itemId: Int): ZoomMode {
	return when (itemId) {
		ZOOM_FIT -> ZoomMode.ZOOM_FIT
		ZOOM_AUTO_CROP -> ZoomMode.AUTO_CROP
		ZOOM_STRETCH -> ZoomMode.STRETCH
		else -> {
			Timber.e("Unknown zoom ID")
			ZoomMode.ZOOM_FIT
		}
	}
}

