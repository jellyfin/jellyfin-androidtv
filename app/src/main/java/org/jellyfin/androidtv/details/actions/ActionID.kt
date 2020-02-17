package org.jellyfin.androidtv.details.actions

enum class ActionID(val id: Long) {
	RESUME(0),
	PLAY_FROM_BEGINNING(1),
	TOGGLE_WATCHED(2),
	TOGGLE_FAVORITE(3),
	ADD_TO_QUEUE(4),
	DELETE(5),
	SECONDARIES_ACTION_POPUP(6)
}
