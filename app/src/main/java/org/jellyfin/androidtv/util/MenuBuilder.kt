package org.jellyfin.androidtv.util

import android.content.Context
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.GravityInt

class MenuBuilder(
	private val menu: Menu
) {
	fun item(
		title: String,
		onClick: () -> Unit
	): MenuItem = menu.add(title).apply {
		setOnMenuItemClickListener {
			onClick()
			true
		}
	}

	fun subMenu(title: String, init: MenuBuilder.() -> Unit): SubMenu = menu.addSubMenu(title).apply {
		MenuBuilder(this).init()
	}
}

fun popupMenu(
	context: Context,
	anchor: View,
	@GravityInt gravity: Int = Gravity.NO_GRAVITY,
	init: MenuBuilder.() -> Unit
): PopupMenu = PopupMenu(context, anchor, gravity).apply {
	MenuBuilder(menu).init()
}

fun PopupMenu.showIfNotEmpty() = if (menu.hasVisibleItems()) {
	show()
	true
} else false
