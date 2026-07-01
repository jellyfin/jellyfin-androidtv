package org.jellyfin.androidtv.util

import android.os.Build

/**
 * Helper to check the current Android version.
 *
 * Comparisons are made against the device's Android SDK version in [Build.VERSION.SDK_INT]. Using the named
 * properties below reads better at the call site than an inline `Build.VERSION.SDK_INT >= Build.VERSION_CODES.X`
 * comparison.
 *
 * The [sdkInt] getter is the single seam that reads the system value. The version checks build on it, so tests can
 * mock the SDK version by mocking [sdkInt] alone (`mockkObject(AndroidVersion)` + `every { AndroidVersion.sdkInt }`).
 * This indirection exists because [Build.VERSION.SDK_INT] is a static final field that can't be set reflectively on
 * JDK 17+.
 *
 * @see Build.VERSION.SDK_INT
 */
object AndroidVersion {
	/**
	 * The device's Android SDK version. Wraps [Build.VERSION.SDK_INT] so it can be mocked in tests.
	 */
	val sdkInt: Int get() = Build.VERSION.SDK_INT

	/** At least Android 7 Nougat, API 24. @see Build.VERSION_CODES.N */
	val isAtLeastN: Boolean get() = sdkInt >= Build.VERSION_CODES.N

	/** At least Android 8 Oreo, API 26. @see Build.VERSION_CODES.O */
	val isAtLeastO: Boolean get() = sdkInt >= Build.VERSION_CODES.O

	/** At least Android 9 Pie, API 28. @see Build.VERSION_CODES.P */
	val isAtLeastP: Boolean get() = sdkInt >= Build.VERSION_CODES.P

	/** At least Android 10 Q, API 29. @see Build.VERSION_CODES.Q */
	val isAtLeastQ: Boolean get() = sdkInt >= Build.VERSION_CODES.Q

	/** At least Android 11 R, API 30. @see Build.VERSION_CODES.R */
	val isAtLeastR: Boolean get() = sdkInt >= Build.VERSION_CODES.R

	/** At least Android 12 S, API 31. @see Build.VERSION_CODES.S */
	val isAtLeastS: Boolean get() = sdkInt >= Build.VERSION_CODES.S

	/** At least Android 13 Tiramisu, API 33. @see Build.VERSION_CODES.TIRAMISU */
	val isAtLeastT: Boolean get() = sdkInt >= Build.VERSION_CODES.TIRAMISU

	/** At least Android 14 Upside Down Cake, API 34. @see Build.VERSION_CODES.UPSIDE_DOWN_CAKE */
	val isAtLeastUpsideDownCake: Boolean get() = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

	/** At least Android 16 Baklava, API 36. @see Build.VERSION_CODES.BAKLAVA */
	val isAtLeastBaklava: Boolean get() = sdkInt >= Build.VERSION_CODES.BAKLAVA
}
