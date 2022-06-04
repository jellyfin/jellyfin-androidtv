package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.androidtv.data.model.AppNotification

interface NotificationsRepository {
	val notifications: StateFlow<List<AppNotification>>

	fun dismissNotification(item: AppNotification)
}

class NotificationsRepositoryImpl : NotificationsRepository {
	override val notifications = MutableStateFlow(emptyList<AppNotification>())

	override fun dismissNotification(item: AppNotification) {
		notifications.value = notifications.value.filter { it != item }
	}
}
