package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.CodecTypes
import org.jellyfin.androidtv.constant.ContainerTypes
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DeviceProfile
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.SubtitleProfile
import org.jellyfin.apiclient.model.dlna.TranscodingProfile
import timber.log.Timber

object ProfileHelper {
	private val MediaTest = MediaCodecCapabilitiesTest()

	val deviceHevcCodecProfile = CodecProfile().apply {
		type = CodecType.Video
		codec = CodecTypes.HEVC

		conditions = if (!MediaTest.supportsHevc()) {
			// The following condition is a method to exclude all HEVC
			Timber.i("*** Does NOT support HEVC")
			arrayOf(
				ProfileCondition(
					ProfileConditionType.Equals,
					ProfileConditionValue.VideoProfile,
					"none"
				)
			)
		} else if (!MediaTest.supportsHevcMain10()) {
			Timber.i("*** Does NOT support HEVC 10 bit")
			arrayOf(
				ProfileCondition(
					ProfileConditionType.NotEquals,
					ProfileConditionValue.VideoProfile,
					"Main 10"
				)
			)
		} else {
			// supports all HEVC
			Timber.i("*** Supports HEVC 10 bit")
			arrayOf(
				ProfileCondition(
					ProfileConditionType.NotEquals,
					ProfileConditionValue.VideoProfile,
					"none"
				)
			)
		}
	}

	@JvmStatic
	fun addAc3Streaming(profile: DeviceProfile, primary: Boolean) {
		if (Utils.downMixAudio()) return

		val mkvProfile = findTranscodingProfile(profile, ContainerTypes.MKV)
		if (mkvProfile != null) {
			Timber.i("*** Adding AC3 as supported transcoded audio")
			mkvProfile.audioCodec = if (primary) {
				"${CodecTypes.AC3},${mkvProfile.audioCodec}"
			} else {
				"${mkvProfile.audioCodec},${CodecTypes.AC3}"
			}
		}
	}

	private fun findTranscodingProfile(deviceProfile: DeviceProfile, container: String) = (
		deviceProfile.transcodingProfiles.find { it.container == container }
	)

	internal fun subtitleProfile(format: String, method: SubtitleDeliveryMethod) = (
		SubtitleProfile().apply {
			this.format = format
			this.method = method
		}
	)
}
