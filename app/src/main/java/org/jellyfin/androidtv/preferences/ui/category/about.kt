package org.jellyfin.androidtv.preferences.ui.category

import android.os.Build
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.category
import org.jellyfin.androidtv.preferences.ui.dsl.staticString
import org.jellyfin.androidtv.util.Utils

fun PreferenceScreen.aboutCategory() = category(R.string.pref_about_title) {
	staticString(R.string.lbl_version, Utils.getVersionString())
	staticString(R.string.pref_device_model, "${Build.MANUFACTURER} ${Build.MODEL}")
}
