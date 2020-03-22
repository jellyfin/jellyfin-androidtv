package org.jellyfin.androidtv.details.actions

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R

class ActionAdapter {
	fun createViewHolder(parent: ViewGroup): ActionViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.action, parent, false)

		return ActionViewHolder(view)
	}

	fun bindViewHolder(viewHolder: ActionViewHolder, action: Action) {
		// Visibility
		action.visible.observe(viewHolder, Observer { visible ->
			viewHolder.view.visibility = if (visible) View.VISIBLE else View.GONE
			viewHolder.button.visibility = if (visible) View.VISIBLE else View.GONE
		})

		// Icon
		action.icon.observe(viewHolder, Observer { icon ->
			viewHolder.button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
		})

		// Text
		action.text.observe(viewHolder, Observer { text ->
			viewHolder.button.text = text
		})

		// Active state
		if (action is ToggleableAction) {
			action.active.observe(viewHolder, Observer { active ->
				val color = viewHolder.button.resources.getColor(if (active) R.color.action_active else R.color.white)
				viewHolder.button.setTextColor(color)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
					viewHolder.button.compoundDrawableTintList = ColorStateList.valueOf(color)
			})
		}

		// Click listener
		viewHolder.button.setOnClickListener { view ->
			GlobalScope.launch(Dispatchers.Main) {
				action.onClick(view)
			}
		}

		// Set state so the observers initialize
		viewHolder.lifecycle.currentState = Lifecycle.State.STARTED
	}

	fun unbindViewHolder(viewHolder: ActionViewHolder) {
		viewHolder.lifecycle.currentState = Lifecycle.State.DESTROYED
	}

	class ActionViewHolder(val view: View) : RecyclerView.ViewHolder(view), LifecycleOwner {
		val button: Button = view.findViewById(R.id.action_button)

		// Lifecycle
		private val lifecycleRegistry = LifecycleRegistry(this)
		override fun getLifecycle() = lifecycleRegistry
	}
}

