package org.jellyfin.androidtv.util.profile

import android.os.Build

/**
 * List of devie models with known HEVC DoVi/HDR10+ playback issues.
 */
private val modelsWithDoViHdr10PlusBug = listOf(
	"AFTKRT", // Amazon Fire TV 4K Max (2nd Gen)
	"AFTKA", // Amazon Fire TV 4K Max (1st Gen)
	"AFTKM", // Amazon Fire TV 4K (2nd Gen)
)

object KnownDefects {
	val hevcDoviHdr10PlusBug = Build.MODEL in modelsWithDoViHdr10PlusBug
}
