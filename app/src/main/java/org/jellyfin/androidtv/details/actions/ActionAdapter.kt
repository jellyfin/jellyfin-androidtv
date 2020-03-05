package org.jellyfin.androidtv.details.actions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.leanback.widget.Action
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R

class ActionAdapter : ObjectAdapter(ActionPresenter()) {
	private val actions = arrayListOf<Action>()
	private var visibleActions = emptyList<Action>()

	fun add(action: Action) {
		// Add action
		actions += action

		// Bind listener
		if (action is BaseAction) action.onVisibilityChanged = ::commit
	}

	fun commit() {
		// Update visible actions
		visibleActions = actions.filter { action -> action !is BaseAction || action.isVisible }

		// Notify the actions have changed
		notifyChanged()
	}

	override fun size() = visibleActions.size
	override fun get(position: Int) = visibleActions.getOrNull(position)

	fun setVisibility(action: BaseAction, visible: Boolean) {
		action.isVisible = visible

		commit()
	}

	private class ActionPresenter : Presenter() {
		override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
			val view = LayoutInflater
				.from(parent.context)
				.inflate(R.layout.action, parent, false)

			return ActionViewHolder(view)
		}

		override fun onBindViewHolder(viewHolder: ViewHolder, action: Any) {
			// Cast types
			action as Action
			viewHolder as ActionViewHolder

			// Set data
			viewHolder.button.setCompoundDrawablesWithIntrinsicBounds(null, action.icon, null, null)
			viewHolder.button.text = action.label1

			if (action is SecondariesPopupAction) {
				action.anchor = viewHolder.button
			}

			if (action is ToggleAction) {
				val color = if (action.active) R.color.action_active else R.color.white

				viewHolder.button.apply {
					action.icon.setTint(resources.getColor(color))
					setTextColor(resources.getColor(color))
				}
			}
		}

		override fun onUnbindViewHolder(viewHolder: ViewHolder) {
			viewHolder as ActionViewHolder

			viewHolder.button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
			viewHolder.secondariesPopupAction = null
		}

		private class ActionViewHolder(view: View) : Presenter.ViewHolder(view) {
			var button: Button = view.findViewById(R.id.action_button)
			var secondariesPopupAction: SecondariesPopupAction? = null
		}
	}
}

