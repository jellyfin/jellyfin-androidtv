package org.jellyfin.androidtv.ui.playback

class PlaybackControllerContainer {
	var playbackController: PlaybackController? = null

	private var episodesPlayedWithoutInterruption = 0
	private var episodeWasInterrupted = false

	fun getEpisodesPlayedWithoutInterruption(): Int {
		return this.episodesPlayedWithoutInterruption
	}

	fun getEpisodeWasInterrupted(): Boolean {
		return this.episodeWasInterrupted
	}

	fun incrementEpisodesPlayedWithoutInterruption() {
		episodesPlayedWithoutInterruption++
	}

	fun resetEpisodesPlayedWithoutInterruption() {
		this.episodesPlayedWithoutInterruption = 0
	}

	fun setEpisodeWasInterrupted(status: Boolean?) {
		this.episodeWasInterrupted = status!!
	}

	fun cancelTimer() {
		playbackController?.cancelTimer()
	}
}
