package org.jellyfin.androidtv.ui.shared

import org.jellyfin.androidtv.constant.CustomMessage

fun interface MessageListener {
	fun onMessageReceived(message: CustomMessage)
}
