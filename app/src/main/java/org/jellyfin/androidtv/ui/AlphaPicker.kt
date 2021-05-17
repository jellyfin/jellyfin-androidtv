package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.AlphaPickerButtonBinding

class AlphaPicker(
	context: Context,
	attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
	var onAlphaSelected: (letter: Char) -> Unit = {}

	init {
		val letters = "#${resources.getString(R.string.byletter_letters)}"
		letters.forEach { letter ->
			// NOTE: We must use a Button here and NOT AppCompatButton due to focus issues on Fire OS
			val binding = AlphaPickerButtonBinding.inflate(LayoutInflater.from(context), this, false)
			binding.button.apply {
				text = letter.toString()
				setOnClickListener { _ ->
					onAlphaSelected(letter)
				}
			}

			addView(binding.root)
		}
	}

	fun focus(letter: Char) {
		children
			.filterIsInstance<Button>()
			.firstOrNull { it.text == letter.toString() }
			?.requestFocus()
	}
}
