package org.jellyfin.androidtv.ui.livetv

import android.widget.RelativeLayout
import java.util.UUID

interface LiveTvGuide {
	fun displayChannels(start: Int, max: Int)
	fun getCurrentLocalStartDate(): Long
	fun showProgramOptions()
	fun setSelectedProgram(programView: RelativeLayout)
	fun refreshFavorite(channelId: UUID)
}
