package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.graphics.Rect
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference

class DurationSeekBarPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle,
	defStyleRes: Int = 0,
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {
	var valueFormatter = ValueFormatter()

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)

		val textView = view.findViewById(androidx.leanback.preference.R.id.seekbar_value) as TextView
		textView.transformationMethod = object : TransformationMethod {
			override fun onFocusChanged(view: View, sourceText: CharSequence?, focused: Boolean, direction: Int, previouslyFocusedRect: Rect) {}

			override fun getTransformation(source: CharSequence?, view: View): CharSequence? {
				val numberValue = source?.toString()?.toIntOrNull() ?: return source
				return valueFormatter.display(numberValue)
			}
		}
	}

	open class ValueFormatter {
		open fun display(value: Int): String = value.toString()
	}
}
