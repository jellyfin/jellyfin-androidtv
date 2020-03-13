package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R

class SecondariesPopupAction(private val context: Context, private val children: List<Action>) : Action {
	override val visible = MediatorLiveData<Boolean>().apply {
		children.forEach {
			addSource(it.visible) {
				value = children.any { child -> child.visible.value == true }
			}
		}
	}
	override val text = MutableLiveData(context.getString(R.string.lbl_more_actions))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_more)!!)

	override suspend fun onClick(view: View) {
		PopupMenu(context, view).apply {
			children
				.filter { it.visible.value == true }
				.forEach { action ->
					menu.add(action.text.value!!).apply {
						icon = action.icon.value!!

						setOnMenuItemClickListener {
							GlobalScope.launch(Dispatchers.Main) {
								action.onClick(it.actionView)
							}

							return@setOnMenuItemClickListener true
						}
					}
				}
		}.show()
	}
}
