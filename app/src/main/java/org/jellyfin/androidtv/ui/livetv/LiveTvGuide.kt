package org.jellyfin.androidtv.ui.livetv

import android.widget.RelativeLayout

interface LiveTvGuide {
	fun displayChannels(start: Int, max: Int)
	fun getCurrentLocalStartDate(): Long
	fun showProgramOptions()
	fun setSelectedProgram(programView: RelativeLayout)
	fun refreshFavorite(channelId: String)
}
