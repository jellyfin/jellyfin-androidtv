package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.androidtv.ui.playback.AudioOutputController
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.koin.java.KoinJavaComponent

class SelectAudioOutputAction (
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val audioOutputController = AudioOutputController(playbackController)
	private val audioOutputs = AudioOutputController.AudioOutputs.values()

	init {
		initializeWithIcon(R.drawable.ic_select_audio_output)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context, view: View
	) {
		val audioOutputMenu = populateMenu(context, view, audioOutputController)

		audioOutputMenu.setOnDismissListener { leanbackOverlayFragment.setFading(true) }

		audioOutputMenu.setOnMenuItemClickListener { menuItem ->
			KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java).set(UserPreferences.audioBehaviour, audioOutputs[menuItem.itemId].output)
			audioOutputController.currentAudioOutput = AudioOutputController.AudioOutputs.fromPreference(
				KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)
					.get(UserPreferences.audioBehaviour))
			playbackController.refreshStream()
			audioOutputMenu.dismiss()
			true
		}

		audioOutputMenu.show()
	}

	private fun populateMenu(
		context: Context,
		view: View,
		audioOutputController: AudioOutputController
	) = PopupMenu(context, view, Gravity.END).apply {
		audioOutputs.forEachIndexed { i, selected ->
			menu.add(0, i, i, if (selected.output == AudioBehavior.DIRECT_STREAM) "Direct Stream" else "Downmix to Stereo")
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(audioOutputs.indexOf(audioOutputController.currentAudioOutput)).isChecked = true
	}

}
