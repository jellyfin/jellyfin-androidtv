package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R

class SecondariesPopupAction(context: Context) : BaseAction(ActionID.SECONDARIES_ACTION_POPUP.id, context) {
	private val containedActions = mutableListOf<BaseAction>()
	var anchor: View? = null

	init {
		label1 = context.getString(R.string.lbl_more_actions)
	}

	fun add(toAdd: BaseAction) {
		containedActions.add(toAdd)
	}

	fun remove(toRemove: BaseAction) {
		containedActions.remove(toRemove)
	}

	override fun onClick() {
		val menu = PopupMenu(context, anchor)
		containedActions.forEach {action ->
			val item = menu.menu.add(action.label1)
			item.setOnMenuItemClickListener {
				action.onClick()
				true
			}
		}

		menu.show()
	}
}
