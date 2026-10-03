package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecList
import android.util.Size
import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.util.profile.codec.Av1CodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.AvcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.HevcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.MediaCodecQuery

class MediaCodecCapabilitiesTest(
	private val softwareCodecsEnabled: Boolean,
) {
	private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }
	private val codecQuery by lazy { MediaCodecQuery(mediaCodecList, softwareCodecsEnabled) }
	private val avc by lazy { AvcCodecCapabilities(codecQuery) }
	private val hevc by lazy { HevcCodecCapabilities(codecQuery) }
	private val av1 by lazy { Av1CodecCapabilities(codecQuery) }

	fun supportsAV1(): Boolean = av1.supportsAv1()

	fun supportsAV1Main10(): Boolean = av1.supportsAv1Main10()

	fun supportsAV1DolbyVision(): Boolean = av1.supportsAv1DolbyVision()

	fun supportsAV1HDR10(): Boolean = av1.supportsAv1HDR10()

	fun supportsAV1HDR10Plus(): Boolean = av1.supportsAv1HDR10Plus()

	fun supportsAC4(): Boolean = codecQuery.hasCodecForMime(MimeTypes.AUDIO_AC4)

	fun supportsAVC(): Boolean = avc.supportsAvc()

	fun supportsAVCHigh10(): Boolean = avc.supportsAvcHigh10()

	fun getAVCMainLevel(): Int = avc.getMainLevel()

	fun getAVCHigh10Level(): Int = avc.getHigh10Level()

	fun supportsHevc(): Boolean = hevc.supportsHevc()

	fun supportsHevcMain10(): Boolean = hevc.supportsHevcMain10()

	fun supportsHevcDolbyVision(): Boolean = hevc.supportsHevcDolbyVision()

	fun supportsHevcDolbyVisionEL(): Boolean = hevc.supportsHevcDolbyVisionEL()

	fun supportsHevcHDR10(): Boolean = hevc.supportsHevcHDR10()

	fun supportsHevcHDR10Plus(): Boolean = hevc.supportsHevcHDR10Plus()

	fun getHevcMainLevel(): Int = hevc.getMainLevel()

	fun getHevcMain10Level(): Int = hevc.getMain10Level()

	fun supportsVc1(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_VC1)

	fun getMaxResolution(mime: String): Size = codecQuery.getMaxResolution(mime)
}
