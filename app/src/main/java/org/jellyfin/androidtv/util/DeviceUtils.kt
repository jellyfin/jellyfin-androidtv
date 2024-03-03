package org.jellyfin.androidtv.util

import android.os.Build

@Deprecated("Device specific code is discouraged and should be removed when possible. Use feature testing instead.")
object DeviceUtils {
	private const val FIRE_TV_PREFIX = "AFT"

	// Fire TV Stick Models
	private const val FIRE_STICK_MODEL_GEN_1 = "AFTM"
	private const val FIRE_STICK_MODEL_GEN_2 = "AFTT"
	private const val FIRE_STICK_MODEL_GEN_3 = "AFTSSS"
	private const val FIRE_STICK_LITE_MODEL = "AFTSS"
	private const val FIRE_STICK_4K_MODEL = "AFTMM"
	private const val FIRE_STICK_4K_MAX_MODEL = "AFTKA"

	// Fire TV (Box) Models
	private const val FIRE_TV_MODEL_GEN_1 = "AFTB"
	private const val FIRE_TV_MODEL_GEN_2 = "AFTS"

	// Fire TV 4k Models
	private const val TOSHIBA_4K_2022 = "AFTHA004" // Toshiba 4K UHD - Fire TV (2022)
	private const val HISENSE_4K_2022 = "AFTHA001" // Hisense U6 4K UHD - Fire TV (2022), Toshiba 4K UHD - Fire TV (2021)
	private const val TOSHIBA_4K_2021 = "AFTHA003" // Toshiba 4K Far-field UHD - Fire TV (2021)

	// Nvidia Shield TV Model
	private const val SHIELD_TV_MODEL = "SHIELD Android TV"

	@JvmStatic
	val isFireTv: Boolean = Build.MODEL.startsWith(FIRE_TV_PREFIX)

	@JvmStatic
	val isFireTv4k: Boolean = Build.MODEL in listOf(
		TOSHIBA_4K_2022,
		HISENSE_4K_2022,
		TOSHIBA_4K_2021,
	)

	@JvmStatic
	val isFireTvStick4k: Boolean = Build.MODEL in listOf(
		FIRE_STICK_4K_MODEL,
		FIRE_STICK_4K_MAX_MODEL,
	)

	@JvmStatic
	val isShieldTv: Boolean = Build.MODEL == SHIELD_TV_MODEL

	@JvmStatic
	// These devices only support a max video resolution of 1080p
	fun has4kVideoSupport(): Boolean = Build.MODEL !in listOf(
		FIRE_STICK_MODEL_GEN_1,
		FIRE_STICK_MODEL_GEN_2,
		FIRE_STICK_MODEL_GEN_3,
		FIRE_STICK_LITE_MODEL,
		FIRE_TV_MODEL_GEN_1,
		FIRE_TV_MODEL_GEN_2
	)
}
