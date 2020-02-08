package org.jellyfin.androidtv.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import org.jellyfin.androidtv.R

class Rating(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
	private val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.Rating, 0, 0)

	private val imageView = ImageView(context).apply {
		id = View.generateViewId()

		setImageResource(R.drawable.ic_star)
		layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
			addRule(ALIGN_PARENT_START, TRUE)
			addRule(ALIGN_PARENT_TOP, TRUE)
		}
	}

	private val textView = TextView(context).apply {
		text = attributes.getText(R.styleable.Rating_text)

		layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
			addRule(ALIGN_TOP, imageView.id)
			addRule(ALIGN_BOTTOM, imageView.id)
			addRule(END_OF, imageView.id)
			marginStart = resources.displayMetrics.density.toInt() * 4
		}
		gravity = Gravity.CENTER_VERTICAL
	}

	var text: CharSequence?
		get() {
			return textView.text
		}
		set(value) {
			textView.text = value
		}

	init {
		addView(imageView)
		addView(textView)
	}
}
