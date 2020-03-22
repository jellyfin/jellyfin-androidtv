package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.PlayableItem

private const val LOG_TAG = "ResumeAction"

class ResumeAction(private val context: Context, val item: LiveData<out PlayableItem>) : PlaybackAction() {
	override val visible = MediatorLiveData<Boolean>().apply {
		addSource(item) { value = it.canResume}
	}
	override val text = MutableLiveData(context.getString(R.string.lbl_resume))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_resume)!!)

	//	override val description = context.getString(R.string.lbl_resume_from, TimeUtils.formatMillis(actualPlaybackPositionInMillis))


	override suspend fun onClick(view: View) {
		Log.i(LOG_TAG, "Resume Clicked!")

		val itemValue = item.value ?: return
		val position = itemValue.playbackPositionTicks / 10000 - TvApp.getApplication().resumePreroll
		playItem(context, itemValue, position, false)
	}
}
