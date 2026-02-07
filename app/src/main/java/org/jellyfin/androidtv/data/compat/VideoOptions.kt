package org.jellyfin.androidtv.data.compat

class VideoOptions : AudioOptions() {
	var audioStreamIndex: Int? = null
	var subtitleStreamIndex: Int? = null

	/**
	 * When true, the server may copy the video stream into the output container (when compatible).
	 * When false, the server should transcode the video stream.
	 */
	var allowVideoStreamCopy: Boolean = true
}
