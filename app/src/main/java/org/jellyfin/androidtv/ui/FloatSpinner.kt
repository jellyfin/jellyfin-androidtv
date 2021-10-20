package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.jellyfin.androidtv.ui.ValueChangedListener
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.NumberSpinnerBinding

class FloatSpinner @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
    var minValue: Float? = null,
    var maxValue: Float? = null,
	private var changeListener: ((value: Float) -> Unit)? = null,
) : FrameLayout(context, attrs) {
    private var currentValue = 1f
    private var increment = .1f
	private val binding: NumberSpinnerBinding = NumberSpinnerBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        if (!isInEditMode) {
        	binding.btnIncrease.setOnClickListener {
                value = if (maxValue == null || currentValue + increment < maxValue!!) {
                    currentValue + increment
                } else {
                    maxValue
                }!!
        	}
        	binding.btnDecrease.setOnClickListener {
                value = if (minValue == null || currentValue - increment > minValue!!) {
                    currentValue - increment
                } else {
                    minValue
                }!!
        	}
        }
    }

    fun setOnChangeListener(listener: ((value: Float) -> Unit)?) {
		changeListener = listener
    }

    var value: Float
        get() = currentValue
        set(value) {
			currentValue = value
			binding.txtValue.text = String.format(resources.configuration.locale, "%.1f", currentValue)
			changeListener?.invoke(value)
        }

    fun setIncrement(value: Float) {
        increment = value
    }
}
