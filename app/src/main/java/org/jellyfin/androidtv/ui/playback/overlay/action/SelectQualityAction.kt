package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.qualityOptions
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQualityController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.koin.java.KoinJavaComponent
import org.koin.java.KoinJavaComponent.get

class SelectQualityAction (
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {

	private var previousQualitySelection = get<UserPreferences>(UserPreferences::class.java)[UserPreferences.maxBitrate]


	private val qualityController = VideoQualityController(previousQualitySelection)
	private val qualityProfiles = qualityOptions.associate {

		val value = when {
			it == 0.0 -> context.getString(R.string.bitrate_auto)
			it >= 1.0 -> context.getString(R.string.bitrate_mbit, it)
			else -> context.getString(R.string.bitrate_kbit, it * 1000.0)
		}

		it.toString().removeSuffix(".0") to value
	}.values

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
			qualityController.currentQuality = qualityProfiles.elementAt(menuItem.itemId)
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
		qualityProfiles.forEachIndexed { i, selected ->
			// Since this is purely numeric data, coerce to en_us to keep the linter happy
			menu.add(0, i, i, selected)
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(qualityProfiles.indexOf(qualityController.currentQuality)).isChecked = true
	}

}
