package org.jellyfin.androidtv.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import org.jellyfin.androidtv.databinding.TextUnderButtonBinding
import org.jellyfin.androidtv.util.dp

class TextUnderButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = TextUnderButtonBinding.inflate(LayoutInflater.from(context), this, true)

	init {
		isFocusable = true
		isFocusableInTouchMode = true
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) defaultFocusHighlightEnabled = false
	}

	fun setLabel(text: String?) {
		binding.label.isVisible = text != null
		binding.label.text = text
		binding.imageButton.contentDescription = text
	}

	fun setIcon(@DrawableRes resource: Int, maxHeight: Int? = null) {
		binding.imageButton.setImageResource(resource)
		binding.imageButton.maxHeight = maxHeight ?: Int.MAX_VALUE
	}

	fun setPadding(padding: Int?) {
		binding.imageButton.setPadding(padding?.dp(context) ?: 0)
	}

	companion object {
		@JvmStatic
		@Suppress("LongParameterList")
		fun create(
			context: Context,
			@DrawableRes icon: Int,
			maxHeight: Int? = null,
			padding: Int? = null,
			label: String? = null,
			onClickListener: OnClickListener
		) = TextUnderButton(context).apply {
			setLabel(label)
			setIcon(icon, maxHeight)
			setPadding(padding)
			setOnClickListener(onClickListener)
		}
	}
}
