package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewButtonServerBinding
import org.jellyfin.androidtv.util.MenuBuilder
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.showIfNotEmpty

class ServerButtonView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewButtonServerBinding.inflate(LayoutInflater.from(context), this, true)

	init {
		isFocusable = true
		isClickable = true
		descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
		foreground = ResourcesCompat.getDrawable(resources, R.drawable.ripple, context.theme)
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

	fun setPopupMenu(init: MenuBuilder.() -> Unit) {
		setOnLongClickListener {
			popupMenu(context, binding.root, init = init).showIfNotEmpty()
		}
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		if (super.onKeyUp(keyCode, event)) return true

		// Menu key should show the popup menu
		if (event.keyCode == KeyEvent.KEYCODE_MENU) return performLongClick()

		return false
	}

	enum class State {
		DEFAULT,
		EDIT,
		CONNECTING,
		ERROR
	}
}
