package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitle
import org.jellyfin.androidtv.onlinesubtitles.OnlineSubtitlesHelper
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.ui.playback.setSubtitleIndex
import org.koin.android.ext.android.inject
import timber.log.Timber

class ShowSubtitleEarlierAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {

	val step : Long = -500

	private var popup: PopupMenu? = null

	val onlineSubtitlesHelper by (context as Activity).inject<OnlineSubtitlesHelper>()

	init {
		initializeWithIcon(R.drawable.ic_subtitle_show_earlier)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)

		removePopup()

		val currentStreamIndex = playbackController.subtitleStreamIndex
		val currentItemId = playbackController.currentlyPlayingItem.id

		val modifiedSubtitle : OnlineSubtitle = onlineSubtitlesHelper.addOffsetToSubtitle(context, currentItemId, currentStreamIndex, step) ?: return
		playbackController.setSubtitleIndex(modifiedSubtitle.localSubtitleId)


		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {

				val sign = if (modifiedSubtitle.offset >= 0) "+" else ""
				add(0, -1, 0, "⏱: " +sign + modifiedSubtitle.offset+ " ms").apply {
					isChecked = false
				}

				setGroupCheckable(0, false, false)
			}
			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { item ->
				playbackController.setSubtitleIndex(item.itemId)
				true
			}
		}
		popup?.show()
		Handler(Looper.getMainLooper()).postDelayed({
			removePopup()
		}, 600)

	}


	fun removePopup() {
		popup?.dismiss()
	}

}
