package org.jellyfin.androidtv.data.repository

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.AppNotification

interface NotificationsRepository {
	val notifications: StateFlow<List<AppNotification>>

	fun dismissNotification(item: AppNotification)
	fun addDefaultNotifications()
}

class NotificationsRepositoryImpl(
	private val context: Context,
	private val uiModeManager: UiModeManager,
) : NotificationsRepository {
	override val notifications = MutableStateFlow(emptyList<AppNotification>())

	override fun dismissNotification(item: AppNotification) {
		notifications.value = notifications.value.filter { it != item }
	}

	override fun addDefaultNotifications() {
		addUiModeNotification()
	}

	private fun addNotification(message: String) {
		notifications.value = notifications.value + AppNotification(message)
	}

	private fun addUiModeNotification() {
		val supportedUiModes = setOf(Configuration.UI_MODE_TYPE_TELEVISION, Configuration.UI_MODE_TYPE_UNDEFINED)
		val invalidUiMode = !supportedUiModes.contains(uiModeManager.currentModeType)
		val isTouch = context.packageManager.hasSystemFeature("android.hardware.touchscreen")
		val hasHdmiCec = context.packageManager.hasSystemFeature("android.hardware.hdmi.cec")

		if (invalidUiMode && isTouch && !hasHdmiCec) addNotification(context.getString(R.string.app_notification_uimode_invalid))
	}
}
