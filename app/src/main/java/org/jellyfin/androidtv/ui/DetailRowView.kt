package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.view.LayoutInflater
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding

class DetailRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewRowDetailsBinding.inflate(LayoutInflater.from(context), this, true)
}
