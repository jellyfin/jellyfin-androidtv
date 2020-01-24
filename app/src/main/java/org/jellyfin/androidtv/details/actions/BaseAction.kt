package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.leanback.widget.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

private const val LOG_TAG = "BaseAction"

abstract class BaseAction(id: Long, protected val context: Context) : Action(id) {
	abstract fun onClick()
}
