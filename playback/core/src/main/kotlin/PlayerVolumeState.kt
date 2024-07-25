package org.jellyfin.playback.core

import android.media.AudioManager
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * The state of the device volume. No flows are used due to platform limitations.
 */
interface PlayerVolumeState {
	/**
	 * Whether the volume of the device is muted or not.
	 */
	val muted: Boolean

	/**
	 * The current volume level of the device between 0f and 1f.
	 */
	val volume: Float

	/**
	 * Whether the volume and mute state can be changed or not.
	 * Changing the volume/mute state will do nothing when false.
	 */
	val modifiable: Boolean

	/**
	 * Mute the device.
	 */
	fun mute()

	/**
	 * Unmute the device.
	 */
	fun unmute()

	/**
	 * Increase the device volume by the device preferred amount.
	 */
	fun increaseVolume()

	/**
	 * Decrease the device volume by the device preferred amount.
	 */
	fun decreaseVolume()

	/**
	 * Set the device volume to a level between 0f and 1f.
	 */
	fun setVolume(volume: Float)
}

/**
 * Implementation of [PlayerVolumeState] that does nothing. Useful for platforms that do not
 * support changing the volume properties.
 */
class NoOpPlayerVolumeState : PlayerVolumeState {
	override val muted = false
	override val volume = 1f
	override val modifiable = false

	override fun mute() = Unit
	override fun unmute() = Unit

	override fun increaseVolume() = Unit
	override fun decreaseVolume() = Unit
	override fun setVolume(volume: Float) = Unit
}

@RequiresApi(Build.VERSION_CODES.M)
class AndroidPlayerVolumeState(
	private val audioManager: AudioManager,
) : PlayerVolumeState {
	private val stream = AudioManager.STREAM_MUSIC

	override val muted: Boolean
		get() = audioManager.isStreamMute(stream)
	override val volume: Float
		get() = audioManager.getStreamVolume(stream).toFloat() / audioManager.getStreamMaxVolume(stream)

	override val modifiable: Boolean
		get() = !audioManager.isVolumeFixed

	override fun mute() {
		if (!modifiable) return
		audioManager.adjustStreamVolume(
			stream,
			AudioManager.ADJUST_MUTE,
			AudioManager.FLAG_SHOW_UI
		)
	}

	override fun unmute() {
		if (!modifiable) return
		audioManager.adjustStreamVolume(
			stream,
			AudioManager.ADJUST_UNMUTE,
			AudioManager.FLAG_SHOW_UI
		)
	}

	override fun increaseVolume() {
		if (!modifiable) return
		audioManager.adjustStreamVolume(
			stream,
			AudioManager.ADJUST_RAISE,
			AudioManager.FLAG_SHOW_UI
		)
	}

	override fun decreaseVolume() {
		if (!modifiable) return
		audioManager.adjustStreamVolume(
			stream,
			AudioManager.ADJUST_LOWER,
			AudioManager.FLAG_SHOW_UI
		)
	}

	override fun setVolume(@FloatRange(0.0, 1.0) volume: Float) {
		require(volume in 0f..1f)
		if (!modifiable) return
		val maxVolume = audioManager.getStreamMaxVolume(stream)
		val index = (volume * maxVolume).roundToInt()
		Timber.d("volume=$volume, maxVolume=$maxVolume, index=$index")
		audioManager.setStreamVolume(stream, index, AudioManager.FLAG_SHOW_UI)
	}
}
