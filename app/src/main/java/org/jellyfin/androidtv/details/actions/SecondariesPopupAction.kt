package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R

class SecondariesPopupAction(context: Context) : BaseAction(ActionID.SECONDARIES_ACTION_POPUP.id, context) {
	private val containedActions = mutableListOf<BaseAction>()
	var anchor: View? = null

	private fun recomputeVisibility() {
		isVisible = containedActions.any { action -> action.isVisible }
	}

	init {
		label1 = context.getString(R.string.lbl_more_actions)
		icon = context.getDrawable(R.drawable.ic_more)
	}

	fun add(toAdd: BaseAction) {
		toAdd.onVisibilityChanged = ::recomputeVisibility
		containedActions.add(toAdd)
		recomputeVisibility()
	}

	fun remove(toRemove: BaseAction) {
		if (toRemove.onVisibilityChanged == ::recomputeVisibility) toRemove.onVisibilityChanged = null
		containedActions.remove(toRemove)
		recomputeVisibility()
	}

	override fun onClick() {
		val menu = PopupMenu(context, anchor)
		containedActions.forEach { action ->
			if (action.isVisible) {
				val item = menu.menu.add(action.label1)
				item.icon = action.icon
				item.setOnMenuItemClickListener {
					action.onClick()
					true
				}
			}
		}

		menu.show()
	}
}
