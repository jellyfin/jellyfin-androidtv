package org.jellyfin.androidtv.util.profile

import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.apiclient.model.dlna.CodecProfile
import org.jellyfin.apiclient.model.dlna.CodecType
import org.jellyfin.apiclient.model.dlna.DirectPlayProfile
import org.jellyfin.apiclient.model.dlna.DlnaProfileType
import org.jellyfin.apiclient.model.dlna.ProfileCondition
import org.jellyfin.apiclient.model.dlna.ProfileConditionType
import org.jellyfin.apiclient.model.dlna.ProfileConditionValue
import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod
import org.jellyfin.apiclient.model.dlna.SubtitleProfile
import timber.log.Timber

object ProfileHelper {
	private val MediaTest by lazy { MediaCodecCapabilitiesTest() }

	val deviceAV1CodecProfile by lazy {
		CodecProfile().apply {
			type = CodecType.Video
			codec = Codec.Video.AV1

			conditions = when {
				!MediaTest.supportsAV1() -> {
					// The following condition is a method to exclude all AV1
					Timber.i("*** Does NOT support AV1")
					arrayOf(
						ProfileCondition(
							ProfileConditionType.Equals,
							ProfileConditionValue.VideoProfile,
							"none"
						)
					)
				}
				!MediaTest.supportsAV1Main10() -> {
					Timber.i("*** Does NOT support AV1 10 bit")
					arrayOf(
						ProfileCondition(
							ProfileConditionType.NotEquals,
							ProfileConditionValue.VideoProfile,
							"Main 10"
						)
					)
				}
				else -> {
					// supports all AV1
					Timber.i("*** Supports AV1 10 bit")
					arrayOf(
						ProfileCondition(
							ProfileConditionType.NotEquals,
							ProfileConditionValue.VideoProfile,
							"none"
						)
					)
				}
			}
		}
	}

	val deviceAVCCodecProfile by lazy {
		buildList {
			when {
				!MediaTest.supportsAVC() -> {
					Timber.i("*** Does NOT support AVC")
					add(
						ProfileCondition(
							ProfileConditionType.Equals,
							ProfileConditionValue.VideoProfile,
							"none"
						)
					)
				}
				!MediaTest.supportsAVCHigh10() -> {
					Timber.i("*** Does NOT support AVC High 10 bit")
					add(
						ProfileCondition(
							ProfileConditionType.NotEquals,
							ProfileConditionValue.VideoProfile,
							"High 10"
						)
					)
				}
				else -> {
					Timber.i("*** Supports AVC High 10 bit")
					add(
						ProfileCondition(
							ProfileConditionType.NotEquals,
							ProfileConditionValue.VideoProfile,
							"none"
						)
					)
				}
			}
		}
	}

	val deviceAVCLevelCodecProfiles by lazy {
		buildList {
			when {
				MediaTest.supportsAVCHigh10() -> {
					add(
						ProfileCondition(
							ProfileConditionType.LessThanEqual,
							ProfileConditionValue.VideoLevel,
							MediaTest.getAVCHigh10Level()
						)
					)
				}
				else -> {
					add(
						ProfileCondition(
							ProfileConditionType.LessThanEqual,
							ProfileConditionValue.VideoLevel,
							MediaTest.getAVCMainLevel()
						)
					)
				}
			}
		}
	}

	val deviceHevcCodecProfile by lazy {
		CodecProfile().apply {
			type = CodecType.Video
			codec = Codec.Video.HEVC

			conditions = when {
				!MediaTest.supportsHevc() -> {
					// The following condition is a method to exclude all HEVC
					Timber.i("*** Does NOT support HEVC")
					arrayOf(
						ProfileCondition(
							ProfileConditionType.Equals,
							ProfileConditionValue.VideoProfile,
							"none"
						)
					)
				}
				!MediaTest.supportsHevcMain10() -> {
					Timber.i("*** Does NOT support HEVC 10 bit")
					arrayOf(
						ProfileCondition(
							ProfileConditionType.NotEquals,
							ProfileConditionValue.VideoProfile,
							"Main 10"
						)
					)
				}
				else -> {
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
		}
	}

	val deviceHevcLevelCodecProfiles by lazy {
		buildList {
			if (MediaTest.supportsHevc()) {
				add(CodecProfile().apply {
					type = CodecType.Video
					codec = Codec.Video.HEVC

					applyConditions = arrayOf(
						ProfileCondition(
							ProfileConditionType.Equals,
							ProfileConditionValue.VideoProfile,
							"Main"
						)
					)

					conditions = arrayOf(
						ProfileCondition(
							ProfileConditionType.LessThanEqual,
							ProfileConditionValue.VideoLevel,
							MediaTest.getHevcMainLevel()
						)
					)
				})

				if (MediaTest.supportsHevcMain10()) {
					add(CodecProfile().apply {
						type = CodecType.Video
						codec = Codec.Video.HEVC

						applyConditions = arrayOf(
							ProfileCondition(
								ProfileConditionType.Equals,
								ProfileConditionValue.VideoProfile,
								"Main 10"
							)
						)

						conditions = arrayOf(
							ProfileCondition(
								ProfileConditionType.LessThanEqual,
								ProfileConditionValue.VideoLevel,
								MediaTest.getHevcMain10Level()
							)
						)
					})
				}
			}
		}
	}

	val max1080pProfileConditions by lazy {
		arrayOf(
			ProfileCondition(
				ProfileConditionType.LessThanEqual,
				ProfileConditionValue.Width,
				"1920"
			),
			ProfileCondition(
				ProfileConditionType.LessThanEqual,
				ProfileConditionValue.Height,
				"1080"
			)
		)
	}

	val photoDirectPlayProfile by lazy {
		DirectPlayProfile().apply {
			type = DlnaProfileType.Photo
			container = arrayOf(
				"jpg",
				"jpeg",
				"png",
				"gif",
				"webp"
			).joinToString(",")
		}
	}

	fun audioDirectPlayProfile(containers: Array<String>) = DirectPlayProfile()
		.apply {
			type = DlnaProfileType.Audio
			container = containers.joinToString(",")
		}

	fun maxAudioChannelsCodecProfile(channels: Int) = CodecProfile()
		.apply {
			type = CodecType.VideoAudio
			conditions = arrayOf(
				ProfileCondition(
					ProfileConditionType.LessThanEqual,
					ProfileConditionValue.AudioChannels,
					channels.toString()
				)
			)
		}

	internal fun subtitleProfile(
		format: String,
		method: SubtitleDeliveryMethod
	) = SubtitleProfile().apply {
		this.format = format
		this.method = method
	}
}
