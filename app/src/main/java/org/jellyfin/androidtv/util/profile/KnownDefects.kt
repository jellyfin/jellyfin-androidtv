package org.jellyfin.androidtv.util.profile

import android.os.Build

/**
 * List of device models with known HEVC DoVi/HDR10+ playback issues.
 */
private val modelsWithDoViHdr10PlusBug = listOf(
	"AFTKRT", // Amazon Fire TV 4K Max (2nd Gen)
	"AFTKA", // Amazon Fire TV 4K Max (1st Gen)
	"AFTKM", // Amazon Fire TV 4K (2nd Gen)
)

/**
 * List of device models that only support HEVC 1080p, even though reported higher-res support.
 */
private val modelsWithHevcMax1080 = listOf(
	"Pi 4 Model B Rev 1.1", // Raspberry Pi 4b
)

object KnownDefects {
	val hevcDoviHdr10PlusBug = Build.MODEL in modelsWithDoViHdr10PlusBug
	val hevcMax1080 = Build.MODEL in modelsWithHevcMax1080
}
