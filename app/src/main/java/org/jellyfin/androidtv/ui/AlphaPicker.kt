package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import org.jellyfin.androidtv.R

class AlphaPicker(
	context: Context,
	attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
	var onAlphaSelected: (letter: Char) -> Unit = {}

	init {
		removeAllViews()

		val letters = "#${resources.getString(R.string.byletter_letters)}"
		letters.forEach { letter ->
			// NOTE: We must use a Button here and NOT AppCompatButton due to focus issues on Fire OS
			val button = View.inflate(context, R.layout.alpha_picker_button, null) as Button
			button.apply {
				text = letter.toString()
				setOnClickListener { _ ->
					onAlphaSelected(letter)
				}
			}

			addView(button)
		}
	}

	fun focus(letter: Char) {
		children
			.filterIsInstance<Button>()
			.firstOrNull { it.text == letter.toString() }
			?.requestFocus()
	}
}
