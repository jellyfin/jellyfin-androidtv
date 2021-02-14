package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.startup.StartupActivity
import timber.log.Timber

/**
 * ActivityLifecycleCallback that bounces to the StartupActivity if an Activity is created and
 * the currentUser is null.
 */
class AuthenticatedUserCallbacks : AbstractActivityLifecycleCallbacks() {
	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		when (activity) {
			// Ignore startup activities
			is StartupActivity -> return
			is org.acra.dialog.CrashReportDialog -> return
			// All other activities should have a current user
			else -> {
				TvApp.getApplication().apply {
					if (currentUser == null) {
						Timber.w("Current user is null, bouncing to StartupActivity")
						activity.startActivity(Intent(this, StartupActivity::class.java))
						activity.finish()
					}
				}
			}
		}
	}
}
