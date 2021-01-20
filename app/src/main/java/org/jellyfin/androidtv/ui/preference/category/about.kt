package org.jellyfin.androidtv.ui.preference.category

import android.os.Build
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.info
import org.jellyfin.androidtv.util.Utils

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	info {
		setTitle(R.string.lbl_version)
		content = Utils.getVersionString(context)
	}

	info {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
	}
}
