package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.content.Intent
import android.drm.DrmStore
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import kotlinx.android.synthetic.main.text_under_button.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

private const val LOG_TAG = "ResumeAction"

class ResumeAction(context: Context, val playbackPositionTicks: Long, val itemID: String) : PlaybackAction(ActionID.RESUME.id, context) {
	val actualPlaybackPositionInMillis: Long

	init {
		this.actualPlaybackPositionInMillis = playbackPositionTicks / 10000 - TvApp.getApplication().resumePreroll
		this.label1 = context.getString(R.string.lbl_resume_from, TimeUtils.formatMillis(actualPlaybackPositionInMillis))
	}

	override fun onClick() {
		Log.i(LOG_TAG, "Resume Clicked!")
		GlobalScope.launch(Dispatchers.Main) {
			playItemWithID(itemID, actualPlaybackPositionInMillis, false)
		}
	}

}
