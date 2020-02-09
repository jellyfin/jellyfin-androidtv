package org.jellyfin.androidtv.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.dp
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class Rating(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
	private val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.Rating, 0, 0)

	private val imageView = ImageView(context).apply {
		id = View.generateViewId()

		layoutParams = LayoutParams(24.dp, 24.dp).apply {
			addRule(ALIGN_PARENT_START, TRUE)
			addRule(ALIGN_PARENT_TOP, TRUE)
		}
	}

	private val textView = TextView(context).apply {
		layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
			addRule(ALIGN_TOP, imageView.id)
			addRule(ALIGN_BOTTOM, imageView.id)
			addRule(END_OF, imageView.id)
			marginStart = resources.displayMetrics.density.toInt() * 4
		}
		gravity = Gravity.CENTER_VERTICAL
	}

	private fun invalidateData() {
		textView.text = value.toString()

		when (type) {
			RatingType.COMMUNITY -> {
				textView.text = ((value * 10.0f).roundToInt() / 10.0f).toString()

				imageView.setImageResource(R.drawable.ic_star)
			}
			RatingType.CRITICS -> {
				textView.text = value.roundToInt().toString()

				if (value >= 60) imageView.setImageResource(R.drawable.ic_rt_rotten)
				else imageView.setImageResource(R.drawable.ic_rt_fresh)
			}
		}
	}

	var type by Delegates.observable(RatingType.fromIndex(attributes.getInteger(R.styleable.Rating_type, 0))!!, { _, _, _ -> invalidateData() })
	var value by Delegates.observable(attributes.getFloat(R.styleable.Rating_value, 0f), { _, _, _ -> invalidateData() })

	init {
		addView(imageView)
		addView(textView)

		invalidateData()
	}

	enum class RatingType(val index: Int) {
		COMMUNITY(0),
		CRITICS(1);

		companion object {
			fun fromIndex(index: Int) = values().find { it.index == index }
		}
	}
}
