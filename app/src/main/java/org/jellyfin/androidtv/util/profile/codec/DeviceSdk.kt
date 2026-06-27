package org.jellyfin.androidtv.util.profile.codec

import android.os.Build

// Wraps Build.VERSION.SDK_INT so tests can mock it via mockkObject — the static final field
// can't be set reflectively on JDK 17+.
internal object DeviceSdk {
	val sdkInt: Int get() = Build.VERSION.SDK_INT
}
