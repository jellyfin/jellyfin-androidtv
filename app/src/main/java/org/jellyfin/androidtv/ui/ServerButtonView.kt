package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
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


	private var _state: State = State.DEFAULT
	var state: State
		get() = _state
		set(value) {
			_state = value

			when (_state) {
				State.DEFAULT -> {
					binding.iconDefault.isVisible = true
					binding.iconDefault.setImageResource(R.drawable.ic_house)
					binding.iconProgress.isVisible = false
				}
				State.EDIT -> {
					binding.iconDefault.isVisible = true
					binding.iconDefault.setImageResource(R.drawable.ic_house_edit)
					binding.iconProgress.isVisible = false
				}
				State.CONNECTING -> {
					binding.iconDefault.isInvisible = true
					binding.iconProgress.isVisible = true
				}
				State.ERROR -> {
					binding.iconDefault.isVisible = true
					binding.iconDefault.setImageResource(R.drawable.ic_error)
					binding.iconProgress.isVisible = false
				}
			}
		}

	enum class State {
		DEFAULT,
		EDIT,
		CONNECTING,
		ERROR
	}
}
