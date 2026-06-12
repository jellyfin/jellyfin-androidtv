package org.jellyfin.androidtv.util.profile.codec

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat

/**
 * AVC (H.264) codec capability detection backed by a [MediaCodecQuery].
 */
class AvcCodecCapabilities(
	private val query: MediaCodecQuery,
) {
	companion object {
		// AVC levels as reported by ffprobe are multiplied by 10, e.g. level 4.1 is 41. Level 1b is set to 9
		internal val LEVEL_MAP: List<Pair<Int, Int>> = listOf(
			CodecProfileLevel.AVCLevel1b to 9,
			CodecProfileLevel.AVCLevel1 to 10,
			CodecProfileLevel.AVCLevel11 to 11,
			CodecProfileLevel.AVCLevel12 to 12,
			CodecProfileLevel.AVCLevel13 to 13,
			CodecProfileLevel.AVCLevel2 to 20,
			CodecProfileLevel.AVCLevel21 to 21,
			CodecProfileLevel.AVCLevel22 to 22,
			CodecProfileLevel.AVCLevel3 to 30,
			CodecProfileLevel.AVCLevel31 to 31,
			CodecProfileLevel.AVCLevel32 to 32,
			CodecProfileLevel.AVCLevel4 to 40,
			CodecProfileLevel.AVCLevel41 to 41,
			CodecProfileLevel.AVCLevel42 to 42,
			CodecProfileLevel.AVCLevel5 to 50,
			CodecProfileLevel.AVCLevel51 to 51,
			CodecProfileLevel.AVCLevel52 to 52,
		)

		private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
	}

	fun supportsAvc(): Boolean = query.hasCodecForMime(MIME)

	fun supportsAvcHigh10(): Boolean = query.hasDecoder(
		MIME,
		CodecProfileLevel.AVCProfileHigh10,
		CodecProfileLevel.AVCLevel4,
	)

	fun getMainLevel(): Int = getLevel(CodecProfileLevel.AVCProfileMain)

	fun getHigh10Level(): Int = getLevel(CodecProfileLevel.AVCProfileHigh10)

	private fun getLevel(profile: Int): Int {
		val level = query.getDecoderLevel(MIME, profile)

		return LEVEL_MAP.asReversed().find { (codecLevel, _) ->
			level >= codecLevel
		}?.second ?: 0
	}
}
