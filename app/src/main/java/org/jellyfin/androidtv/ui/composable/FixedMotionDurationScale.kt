package org.jellyfin.androidtv.ui.composable

import androidx.compose.ui.MotionDurationScale

/**
 * A [MotionDurationScale] implementation that always returns a fixed scale factor of 1f. To be used for animations that should ignore the
 * system animator duration scale.
 */
object FixedMotionDurationScale : MotionDurationScale {
	override val scaleFactor: Float = 1f
}
