package org.jellyfin.androidtv.ui.preference.category

import android.os.Build
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.screen.LicensesScreen
import org.jellyfin.androidtv.util.Utils

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	link {
		setTitle(R.string.lbl_version)
		content = Utils.getVersionString(context)
	}

	link {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
	}

	link {
		setTitle(R.string.licenses_link)
		setContent(R.string.licenses_link_description)
		withFragment<LicensesScreen>()
	}
}
