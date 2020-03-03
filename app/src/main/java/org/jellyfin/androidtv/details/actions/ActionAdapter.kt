package org.jellyfin.androidtv.details.actions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R

class ActionAdapter : RecyclerView.Adapter<ActionAdapter.ActionViewHolder>() {
	private val actions = arrayListOf<Action>()

	fun add(action: Action) {
		// Add action
		actions += action

		// Bind listener
		action.setChangeListener(::notifyDataSetChanged)

		notifyDataSetChanged()
	}

	override fun getItemCount() = actions.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.action, parent, false)

		return ActionViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ActionViewHolder, position: Int) {
		// Find action
		val action = actions[position]

		// Set data
		viewHolder.view.apply {
			visibility = if (action.visible) View.VISIBLE else View.GONE
		}

		viewHolder.button.apply {
			setCompoundDrawablesWithIntrinsicBounds(null, action.icon, null, null)
			text = action.text

			setOnClickListener { action.onClick() }
		}

		if (action is SecondariesPopupAction) action.anchor = viewHolder.button

		if (action is ToggleAction) {
			val color = if (action.active) R.color.action_active else R.color.white

			viewHolder.button.apply {
				action.icon.setTint(resources.getColor(color))
				setTextColor(resources.getColor(color))
			}
		}
	}

	class ActionViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
		var button: Button = view.findViewById(R.id.action_button)
	}
}

