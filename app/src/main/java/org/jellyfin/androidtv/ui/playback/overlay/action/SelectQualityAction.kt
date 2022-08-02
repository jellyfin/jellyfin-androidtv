package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQualityController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.koin.java.KoinJavaComponent

class SelectQualityAction (
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val qualityController = VideoQualityController(playbackController)
	private val qualityProfiles = VideoQualityController.QualityProfiles.values()

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
			KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java).set(UserPreferences.maxBitrate, qualityProfiles[menuItem.itemId].quality)
			qualityController.currentQuality = VideoQualityController.QualityProfiles.fromPreference(
				KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
					.get(UserPreferences.maxBitrate))
			playbackController.refreshStream()
			qualityMenu.dismiss()
			true
		}

		qualityMenu.show()
	}

	private fun formatQuality(quality: String, context: Context): String {

		val conv = quality.toDouble()

		val value = when {
			conv == 0.0 -> context.getString(R.string.bitrate_auto)
			conv >= 1.0 -> context.getString(R.string.bitrate_mbit, conv)
			else -> context.getString(R.string.bitrate_kbit, conv * 100.0)
		}

		conv.toString().removeSuffix(".0") to value

		return value
	}

	private fun populateMenu(
		context: Context,
		view: View,
		qualityController: VideoQualityController
	) = PopupMenu(context, view, Gravity.END).apply {
		qualityProfiles.forEachIndexed { i, selected ->
			// Since this is purely numeric data, coerce to en_us to keep the linter happy
			menu.add(0, i, i, formatQuality(selected.quality, context))
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(qualityProfiles.indexOf(qualityController.currentQuality)).isChecked = true
	}

}
