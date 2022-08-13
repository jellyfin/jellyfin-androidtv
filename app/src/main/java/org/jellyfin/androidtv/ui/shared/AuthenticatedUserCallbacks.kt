package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.content.Intent
import android.os.Build
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
			// Screensaver activity is not exposed in Android SDK
			"android.service.dreams.DreamActivity"
		)
	}

	override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activity.checkAuthentication()
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.checkAuthentication()
	}

	private fun Activity.checkAuthentication() {
		val name = this::class.qualifiedName

		if (name in ignoredClassNames) {
			Timber.i("Activity $name is ignored")
		} else if (sessionRepository.currentSession.value == null) {
			Timber.w("Activity $name started without a session, bouncing to StartupActivity")
			startActivity(Intent(this, StartupActivity::class.java))
			finish()
		}
	}
}
