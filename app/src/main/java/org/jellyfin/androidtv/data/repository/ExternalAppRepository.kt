package org.jellyfin.androidtv.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.componentName

class ExternalAppRepository(
	private val userPreferences: UserPreferences,
) {
	companion object {
		const val SAMPLE_VIDEO_URL = "http://jellyfin.local/query.mp4"
		const val MEDIA_TYPE_VIDEO = "video/*"
	}

	private val externalPlayerAppIntent = Intent(Intent.ACTION_VIEW).apply {
		setDataAndTypeAndNormalize(SAMPLE_VIDEO_URL.toUri(), MEDIA_TYPE_VIDEO)
	}

	fun getExternalPlayerApps(context: Context): List<ResolveInfo> = context.packageManager
		.queryIntentActivities(externalPlayerAppIntent, 0)
		// Hide apps with priority below zero (system stubs)
		.filter { it.priority >= 0 }

	fun getCurrentExternalPlayerApp(context: Context): ActivityInfo? {
		// Validate if external app should be used at all
		val useExternalPlayer = userPreferences[UserPreferences.useExternalPlayer]
		if (!useExternalPlayer) return null

		// Resolve external app information
		val resolvedInfo = userPreferences[UserPreferences.externalPlayerComponentName]
			.takeIf { it.isNotEmpty() }
			?.let(ComponentName::unflattenFromString)
			?.runCatching { context.packageManager.getActivityInfo(this, 0) }
			?.getOrNull()
		if (resolvedInfo != null) return resolvedInfo

		// Fallback in case the app is uninstalled or unavailable for some other reason
		val externalApps = getExternalPlayerApps(context)

		// System default
		val systemDefault = externalApps.find { it.isDefault }?.activityInfo
		if (systemDefault != null) return systemDefault

		// First compatible, or none
		return externalApps.firstOrNull()?.activityInfo
	}

	fun setExternalPlayerapp(activityInfo: ActivityInfo?) {
		if (activityInfo == null) {
			userPreferences[UserPreferences.useExternalPlayer] = false
			userPreferences[UserPreferences.externalPlayerComponentName] = ""
		} else {
			userPreferences[UserPreferences.useExternalPlayer] = true
			userPreferences[UserPreferences.externalPlayerComponentName] = activityInfo.componentName.flattenToShortString()
		}
	}
}
