package org.jellyfin.androidtv.ui

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewNowPlayingBinding
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingActivity
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.entities.ImageType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NowPlayingView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = R.style.Button_Default,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	val binding = ViewNowPlayingBinding.inflate(LayoutInflater.from(context), this, true)

	private val mediaManager by inject<MediaManager>()
	private var currentDuration: String = ""

	init {
		setPadding(0)

		if (!isInEditMode) setOnClickListener {
			val intent = Intent(context, AudioNowPlayingActivity::class.java)
			context.startActivity(intent)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		if (!isInEditMode) {
			// hook our events
			mediaManager.addAudioEventListener(audioEventListener)

			if (mediaManager.hasAudioQueueItems()) {
				isVisible = true
				setInfo(mediaManager.currentAudioItem)
				setStatus(mediaManager.currentAudioPosition)
			} else isVisible = false
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()

		if (!isInEditMode) mediaManager.removeAudioEventListener(audioEventListener)
	}

	private fun setInfo(item: BaseItemDto) {
		val placeholder = ContextCompat.getDrawable(context, R.drawable.ic_album)
		val blurHash = item.imageBlurHashes?.get(ImageType.Primary)?.get(item.imageTags?.get(ImageType.Primary))
		binding.npIcon.load(ImageUtils.getPrimaryImageUrl(item), blurHash, placeholder, item.primaryImageAspectRatio ?: 1.0)

		currentDuration = TimeUtils.formatMillis(if (item.runTimeTicks != null) item.runTimeTicks / 10_000 else 0)
		binding.npDesc.text = if (item.albumArtist != null) item.albumArtist else item.name
	}

	private fun setStatus(pos: Long) {
		binding.npStatus.text = resources.getString(R.string.lbl_status, TimeUtils.formatMillis(pos), currentDuration)
	}

	fun showDescription(show: Boolean) {
		binding.npDesc.isVisible = show
	}

	private var audioEventListener: AudioEventListener = object : AudioEventListener {
		override fun onPlaybackStateChange(newState: PlaybackController.PlaybackState?, currentItem: BaseItemDto?) {
			when {
				currentItem == null -> Unit
				newState == PlaybackController.PlaybackState.PLAYING -> setInfo(currentItem)
				newState == PlaybackController.PlaybackState.IDLE && isShown -> setStatus(mediaManager.currentAudioPosition)
			}
		}

		override fun onProgress(pos: Long) {
			if (isShown) setStatus(pos)
		}

		override fun onQueueStatusChanged(hasQueue: Boolean) {
			isVisible = hasQueue

			if (hasQueue) {
				// may have just added one so update display
				setInfo(mediaManager.currentAudioItem)
				setStatus(mediaManager.currentAudioPosition)
			}
		}
	}
}
