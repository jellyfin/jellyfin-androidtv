package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.qualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQualityController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class SelectQualityAction (
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	userPreferences: UserPreferences
) : CustomAction(context, customPlaybackTransportControlGlue) {

	private var previousQualitySelection = userPreferences[UserPreferences.maxBitrate]
	private val qualityController = VideoQualityController(previousQualitySelection, userPreferences)
	private val qualityProfiles = qualityProfiles(context)

	init {
		initializeWithIcon(R.drawable.ic_select_quality)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context, view: View
	) {
		val qualityMenu = populateMenu(context, view, qualityController)

		qualityMenu.setOnDismissListener { leanbackOverlayFragment.setFading(true) }

		qualityMenu.setOnMenuItemClickListener { menuItem ->
			qualityController.currentQuality = qualityProfiles.keys.elementAt(menuItem.itemId)
			playbackController.refreshStream()
			qualityMenu.dismiss()
			true
		}

		qualityMenu.show()
	}

	private fun populateMenu(
		context: Context,
		view: View,
		qualityController: VideoQualityController
	) = PopupMenu(context, view, Gravity.END).apply {
		qualityProfiles.values.forEachIndexed { i, selected ->
			menu.add(0, i, i, selected)
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(qualityProfiles.keys.indexOf(qualityController.currentQuality)).isChecked = true
	}

}
