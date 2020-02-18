package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.util.TimeUtils

private const val LOG_TAG = "ResumeAction"

class ResumeAction(context: Context, val item: PlayableItem) : PlaybackAction(ActionID.RESUME.id, context) {
	val actualPlaybackPositionInMillis: Long by lazy {
		item.playbackPositionTicks / 10000 - TvApp.getApplication().resumePreroll
	}

	init {
		this.label1 = context.getString(R.string.lbl_resume_from, TimeUtils.formatMillis(actualPlaybackPositionInMillis))
		icon = context!!.getDrawable(R.drawable.ic_resume)
		isVisible = item.canResume
	}

	override fun onClick() {
		Log.i(LOG_TAG, "Resume Clicked!")
		GlobalScope.launch(Dispatchers.Main) {
			playItem(item, actualPlaybackPositionInMillis, false)
		}
	}

}
