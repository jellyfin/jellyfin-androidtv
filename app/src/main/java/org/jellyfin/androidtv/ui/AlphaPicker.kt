package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import org.jellyfin.androidtv.R

class AlphaPicker(
	context: Context,
	attrs: AttributeSet?
) : LinearLayout(context, attrs) {
	var onAlphaSelected: (s: String) -> Unit = {}

	init {
		removeAllViews()

		val letters = "#${resources.getString(R.string.byletter_letters)}"
		letters.toCharArray().forEach {
			// NOTE: We must use a Button here and NOT AppCompatButton due to focus issues on Fire OS
			val button = Button(context)
			button.text = it.toString()
			button.setOnClickListener { _ ->
				this.onAlphaSelected(it.toString())
			}

			addView(button)
		}
	}

	fun focus(text: String?) {
		if (!text.isNullOrBlank()) {
			for (i in 1..childCount) {
				val button = getChildAt(i) as Button
				if (text == button.text) {
					button.requestFocus()
					return
				}
			}
		}
	}
}
