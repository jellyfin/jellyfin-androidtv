package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.util.apiclient.markPlayed
import org.jellyfin.androidtv.util.apiclient.markUnplayed

class ToggleWatchedAction(context: Context, val item: MutableLiveData<out PlayableItem>) : ToggleableAction {
	override val visible = MutableLiveData(true)
	override val text = MutableLiveData(context.getString(R.string.lbl_watched))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_watch)!!)
	override val active = MediatorLiveData<Boolean>().apply {
		addSource(item) { value = it.played }
	}

	override suspend fun onClick(view: View) {
		val itemValue = item.value ?: return
		val application = TvApp.getApplication()

		//todo catch exceptions (show toast?)
		val response = if (itemValue.played) application.apiClient.markUnplayed(itemValue.id, application.currentUser.id)
		else application.apiClient.markPlayed(itemValue.id, application.currentUser.id, null)

		response?.let {
			itemValue.playbackPositionTicks = it.playbackPositionTicks
			itemValue.played = it.played

			item.value = itemValue
		}
	}
}
