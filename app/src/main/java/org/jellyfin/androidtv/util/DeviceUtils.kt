package org.jellyfin.androidtv.util

import android.os.Build

object DeviceUtils {
	// Chromecast with Google TV
	private const val CHROMECAST_GOOGLE_TV = "Chromecast"
	private const val FIRE_TV_PREFIX = "AFT"

	// Fire TV Stick Models
	private const val FIRE_STICK_MODEL_GEN_1 = "AFTM"
	private const val FIRE_STICK_MODEL_GEN_2 = "AFTT"
	private const val FIRE_STICK_MODEL_GEN_3 = "AFTSSS"
	private const val FIRE_STICK_LITE_MODEL = "AFTSS"
	private const val FIRE_STICK_4K_MODEL = "AFTMM"
	private const val FIRE_STICK_4K_MAX_MODEL = "AFTKA"

	// Fire TV Cube Models
	private const val FIRE_CUBE_MODEL_GEN_1 = "AFTA"
	private const val FIRE_CUBE_MODEL_GEN_2 = "AFTR"

	// Fire TV (Box) Models
	private const val FIRE_TV_MODEL_GEN_1 = "AFTB"
	private const val FIRE_TV_MODEL_GEN_2 = "AFTS"
	private const val FIRE_TV_MODEL_GEN_3 = "AFTN"

	// Fire TV 4k Models
	private const val Toshiba_4K_2022 = "AFTHA004"	//Toshiba 4K UHD - Fire TV (2022)
	private const val Hisense_4K_2022 = "AFTHA001"	//Hisense U6 4K UHD - Fire TV (2022), Toshiba 4K UHD - Fire TV (2021)
	private const val Toshiba_4K_2021 = "AFTHA003"	//Toshiba 4K Far-field UHD - Fire TV (2021)

	// Nvidia Shield TV Model
	private const val SHIELD_TV_MODEL = "SHIELD Android TV"
	private const val UNKNOWN = "Unknown"

	// Stub to allow for mock injection
	fun getBuildModel(): String = Build.MODEL ?: UNKNOWN

	@JvmStatic val isChromecastWithGoogleTV: Boolean get() = getBuildModel() == CHROMECAST_GOOGLE_TV
	@JvmStatic val isFireTv: Boolean get() = getBuildModel().startsWith(FIRE_TV_PREFIX)
	@JvmStatic val isFireTv4k: Boolean get() = getBuildModel() in listOf(Toshiba_4K_2022, 
												Hisense_4K_2022, 
												Toshiba_4K_2021)
	@JvmStatic val isFireTvStickGen1: Boolean get() = getBuildModel() == FIRE_STICK_MODEL_GEN_1
	@JvmStatic val isFireTvStick4k: Boolean get() = getBuildModel() in listOf(FIRE_STICK_4K_MODEL, FIRE_STICK_4K_MAX_MODEL)
	@JvmStatic val isShieldTv: Boolean get() = getBuildModel() == SHIELD_TV_MODEL

	@JvmStatic
	fun has4kVideoSupport(): Boolean = getBuildModel() != UNKNOWN && getBuildModel() !in listOf(
		// These devices only support a max video resolution of 1080p
		FIRE_STICK_MODEL_GEN_1,
		FIRE_STICK_MODEL_GEN_2,
		FIRE_STICK_MODEL_GEN_3,
		FIRE_STICK_LITE_MODEL,
		FIRE_TV_MODEL_GEN_1,
		FIRE_TV_MODEL_GEN_2
	)

	@JvmStatic
	fun is60(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}
