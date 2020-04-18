package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper

class InstantMixAction(context: Context, val item: LiveData<out BaseItem>) : PlaybackAction() {
	override val visible = MutableLiveData(true)
	override val text = MutableLiveData(context.getString(R.string.lbl_instant_mix))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_mix)!!)

	override suspend fun onClick(view: View?) {
		val value = item.value ?: return

		PlaybackHelper.playInstantMix(value.id)
	}
}
