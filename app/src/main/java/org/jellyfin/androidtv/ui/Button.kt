package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import org.jellyfin.androidtv.R
import kotlin.properties.Delegates

/**
 * Extends the default button class and adds the following functionality:
 * - Progress bar background
 */
class Button(context: Context, attrs: AttributeSet?) : AppCompatButton(context, attrs) {
	private val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.Button, 0, 0)

	var progressMax by Delegates.observable(attributes.getInteger(R.styleable.Button_progressMax, 0), { _, _, _ -> invalidateProgressData() })
	var progressValue by Delegates.observable(attributes.getInteger(R.styleable.Button_progressValue, 0), { _, _, _ -> invalidateProgressData() })

	private fun invalidateProgressData() {
		if (progressMax == 0 && progressValue == 0) {
			// hide
			println("PROGRESS: 0,0")
		} else {
			// show
			println("PROGRESS: $progressValue,$progressMax")
		}
	}
}
