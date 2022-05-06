package org.jellyfin.androidtv.ui.card

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewCardDefaultBinding
import kotlin.math.roundToInt

class DefaultCardView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	init {
		isFocusable = true
		descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
	}

	val binding = ViewCardDefaultBinding.inflate(LayoutInflater.from(context), this, true)

	var title: String
		get() = binding.label.text.toString()
		set(value) {
			binding.label.text = value
		}

	fun setSize(size: Size) = when (size) {
		Size.SQUARE -> setSize(size.width, size.height)
	}

	private fun setSize(newWidth: Int, newHeight: Int) {
		binding.bannerContainer.updateLayoutParams {
			@Suppress("MagicNumber")
			height = (newHeight * context.resources.displayMetrics.density + 0.5f).roundToInt()
		}

		val horizontalPadding = with(binding.container) { paddingStart + paddingEnd }
		binding.container.updateLayoutParams {
			@Suppress("MagicNumber")
			width = (newWidth * context.resources.displayMetrics.density + 0.5f).roundToInt() + horizontalPadding
		}

		invalidate()
	}

	fun setImage(
		image: String?,
		@DrawableRes placeholder: Int? = null,
	) {
		Glide.with(context)
			.load(image).apply {
				if (placeholder != null)
					placeholder(placeholder)
			}
			.into(binding.banner)
	}

	override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

		val scaleRes = if (gainFocus) R.fraction.card_scale_focus else R.fraction.card_scale_default
		val scale = resources.getFraction(scaleRes, 1, 1)

		post {
			animate().apply {
				scaleX(scale)
				scaleY(scale)
				duration = resources.getInteger(R.integer.card_scale_duration).toLong()
				withLayer()
			}
		}
	}

	@Suppress("MagicNumber")
	enum class Size(val width: Int, val height: Int) {
		SQUARE(110, 110)
	}
}
