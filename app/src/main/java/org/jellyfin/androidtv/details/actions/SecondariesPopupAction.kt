package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R

class SecondariesPopupAction(context: Context) : Action(ActionID.SECONDARIES_ACTION_POPUP.id, context) {
	override val visible = true
	override val text = context.getString(R.string.lbl_more_actions)
	override val icon = context.getDrawable(R.drawable.ic_more)!!

	private val children = mutableListOf<Action>()
	var anchor: View? = null

	fun add(child: Action) {
		child.setChangeListener { notifyDataChanged() }
		children.add(child)

		notifyDataChanged()
	}

	fun remove(child: Action) {
		child.setChangeListener(null)
		children.remove(child)

		notifyDataChanged()
	}

	override fun onClick() {
		PopupMenu(context, anchor).apply {
			children.filter { it.visible }.forEach { action ->
				menu.add(action.text).apply {
					icon = action.icon
					setOnMenuItemClickListener {
						action.onClick()

						return@setOnMenuItemClickListener true
					}
				}
			}
		}.show()
	}
}
