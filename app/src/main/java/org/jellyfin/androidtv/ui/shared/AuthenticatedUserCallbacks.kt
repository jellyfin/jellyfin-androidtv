package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import timber.log.Timber

/**
 * ActivityLifecycleCallback that bounces to the StartupActivity if an Activity is created and
 * the currentUser is null.
 */
class AuthenticatedUserCallbacks : AbstractActivityLifecycleCallbacks() {
	companion object {
		val ignoredClassNames = arrayOf(
			// Startup activities
			StartupActivity::class.qualifiedName,
			PreferencesActivity::class.qualifiedName,
			// Third party
			org.acra.dialog.CrashReportDialog::class.qualifiedName,
			// Screensaver activity is not exposed in Android SDK
			"android.service.dreams.DreamActivity"
		)
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		val name = activity::class.qualifiedName

		if (name in ignoredClassNames) {
			Timber.i("Activity $name is ignored")
			return
		}

		TvApp.getApplication()?.apply {
			if (currentUser == null) {
				Timber.w("Current user is null, bouncing to StartupActivity")
				activity.startActivity(Intent(this, StartupActivity::class.java))
				activity.finish()
			}
		}
	}
}
