package org.jellyfin.playback.core

import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import kotlin.time.Duration

/** User intent, before it changes the local backend (for example, a synchronized group player). */
sealed interface PlayerCommand {
	data object Play : PlayerCommand
	data object Pause : PlayerCommand
	data object Stop : PlayerCommand
	data class Seek(val position: Duration) : PlayerCommand
	data object Next : PlayerCommand
	data object Previous : PlayerCommand
	data class Select(val index: Int) : PlayerCommand
	data class Speed(val speed: Float) : PlayerCommand
	data class Order(val order: PlaybackOrder) : PlayerCommand
	data class Repeat(val mode: RepeatMode) : PlayerCommand
	data class Remove(val index: Int) : PlayerCommand
}

interface PlayerCommandHandler {
	/** Return true when the command was handled and should not run locally. */
	fun handleCommand(command: PlayerCommand): Boolean
}
