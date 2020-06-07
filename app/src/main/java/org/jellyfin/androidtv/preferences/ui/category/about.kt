package org.jellyfin.androidtv.preferences.ui.category

import android.os.Build
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.OptionsScreen
import org.jellyfin.androidtv.preferences.ui.dsl.info
import org.jellyfin.androidtv.util.Utils

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	info {
		setTitle(R.string.lbl_version)
		content = Utils.getVersionString()
	}

	info {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
	}
}
