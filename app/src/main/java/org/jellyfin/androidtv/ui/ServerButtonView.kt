package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.ViewButtonServerBinding

class ServerButtonView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewButtonServerBinding.inflate(LayoutInflater.from(context), this, true)

	init {
		isFocusable = true
		descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
	}

	var name: String
		get() = binding.serverName.text.toString()
		set(value) {
			binding.serverName.text = value
		}

	var address: String
		get() = binding.serverAddress.text.toString()
		set(value) {
			binding.serverAddress.text = value
		}

	var version: String?
		get() = binding.serverVersion.text?.toString()
		set(value) {
			binding.serverVersion.text = value
		}
}
