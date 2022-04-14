package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import timber.log.Timber

/**
 * ActivityLifecycleCallback that bounces to the StartupActivity when an activity is created and
 * the currentUser is null.
 */
class AuthenticatedUserCallbacks(
	private val sessionRepository: SessionRepository,
) : AbstractActivityLifecycleCallbacks() {
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

	override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
		val name = activity::class.qualifiedName

		if (name in ignoredClassNames) {
			Timber.i("Activity $name is ignored")
		} else if (sessionRepository.currentSession.value == null) {
			Timber.w("Activity $name started without a session, bouncing to StartupActivity")
			activity.startActivity(Intent(activity, StartupActivity::class.java))
			activity.finish()
		}
	}
}
