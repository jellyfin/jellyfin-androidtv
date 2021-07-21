package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.os.Bundle
import org.jellyfin.androidtv.JellyfinApplication

class CurrentActivityCallbacks : AbstractActivityLifecycleCallbacks() {
	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		super.onActivityCreated(activity, savedInstanceState)

		(activity.application as? JellyfinApplication)?.currentActivity = activity
	}

	override fun onActivityResumed(activity: Activity) {
		super.onActivityResumed(activity)

		(activity.application as? JellyfinApplication)?.currentActivity = activity
	}

	override fun onActivityPaused(activity: Activity) {
		super.onActivityPaused(activity)
		(activity.application as? JellyfinApplication)?.currentActivity = null
	}
}
