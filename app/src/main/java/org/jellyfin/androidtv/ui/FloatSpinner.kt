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

class FloatSpinner : FrameLayout {
    var mValue = 1f
    var mIncrement = .1f
    var mValueChangedListener: ValueChangedListener<Float>? = null
	private lateinit var binding: NumberSpinnerBinding;

    constructor(context: Context, listener: ValueChangedListener<Float>?) : super(context) {
        mValueChangedListener = listener
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val inflater = LayoutInflater.from(context)
		binding = NumberSpinnerBinding.inflate(inflater, this, true)
        if (!isInEditMode) {
        	binding.btnIncrease.setOnClickListener { value = mValue + mIncrement }
        	binding.btnDecrease.setOnClickListener { value = mValue - mIncrement }
        }
    }

    fun setOnChangeListener(listener: ValueChangedListener<Float>?) {
        mValueChangedListener = listener
    }

    var value: Float
        get() = mValue
        set(value) {
            mValue = value
			binding.txtValue.text = String.format(resources.configuration.locale, "%.1f", mValue)
            if (mValueChangedListener != null) {
                mValueChangedListener!!.onValueChanged(value)
            }
        }

    fun setIncrement(value: Float) {
        mIncrement = value
    }
}
