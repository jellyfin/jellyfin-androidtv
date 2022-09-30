package org.jellyfin.androidtv.constant

sealed interface CustomMessage {
	object RefreshCurrentItem : CustomMessage
	object ActionComplete : CustomMessage
}
