package org.jellyfin.androidtv.util.profile

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import org.jellyfin.androidtv.util.profile.codec.Av1CodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.AvcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.HevcCodecCapabilities
import org.jellyfin.androidtv.util.profile.codec.MediaCodecQuery

// MimeTypes.VIDEO_VP8 and MimeTypes.VIDEO_VP9 are marked as unstable media3 APIs
@OptIn(UnstableApi::class)
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

	fun supportsVp8(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_VP8)

	fun supportsVp9(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_VP9)

	// MPEG-2 decoders also handle MPEG-1, Android does not register a separate MPEG-1 mime type
	fun supportsMpeg2(): Boolean = codecQuery.hasCodecForMime(MimeTypes.VIDEO_MPEG2)

	// DivX/Xvid need Advanced Simple Profile, a Simple Profile only decoder cannot play them
	fun supportsMpeg4Asp(): Boolean =
		codecQuery.getDecoderLevel(MimeTypes.VIDEO_MP4V, CodecProfileLevel.MPEG4ProfileAdvancedSimple) > 0

	fun getMaxResolution(mime: String): Size = codecQuery.getMaxResolution(mime)
}
