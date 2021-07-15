package org.jellyfin.androidtv.util

import android.graphics.RectF
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import com.flaviofaria.kenburnsview.Transition
import com.flaviofaria.kenburnsview.TransitionGenerator
import java.security.SecureRandom

class KenBurnsTransitionGenerator(
	var duration: Long = 10_000, // 10 seconds
	var interpolator: Interpolator = AccelerateDecelerateInterpolator(),
) : TransitionGenerator {
	private val random = SecureRandom()
	private var transitioned = false

	override fun generateNextTransition(drawableBounds: RectF, viewport: RectF): Transition {
		val fullRect = createFullRect(drawableBounds, viewport)
		if (transitioned) return Transition(fullRect, fullRect, duration, interpolator)
		transitioned = true

		val partialRect = createPartialRect(fullRect)

		return Transition(partialRect, fullRect, duration, interpolator)
	}

	fun reset() {
		transitioned = false
	}

	private fun createPartialRect(rect: RectF): RectF {
		val scale = 0.75f + random.nextFloat() * 0.20f // Between 0.75f and 0.95f

		val newWidth = rect.width() * scale
		val newHeight = rect.height() * scale

		val offsetLeft = random.nextFloat() * (rect.width() - newWidth)
		val offsetTop = random.nextFloat() * (rect.height() - newHeight)

		return RectF(
			rect.left + offsetLeft,
			rect.top + offsetTop,
			rect.left + newWidth + offsetLeft,
			rect.top + newHeight + offsetTop,
		)
	}

	private fun createFullRect(drawableBounds: RectF, viewport: RectF) =
		if (drawableBounds.ratio > viewport.ratio) {
			val r = drawableBounds.height() / viewport.height() * viewport.width()
			val b = drawableBounds.height()
			val c = (drawableBounds.width() - r) / 2

			RectF(c, 0f, r + c, b)
		} else {
			val r = drawableBounds.width()
			val b = drawableBounds.width() / viewport.width() * viewport.height()
			val c = (drawableBounds.height() - b) / 2

			RectF(0f, c, r, c + b)
		}

	private val RectF.ratio: Float
		get() = width() / height()
}
