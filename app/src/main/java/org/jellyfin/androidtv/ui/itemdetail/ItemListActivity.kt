package org.jellyfin.androidtv.ui.itemdetail

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import org.jellyfin.androidtv.R

class ItemListActivity : FragmentActivity(R.layout.fragment_content_view) {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager.commit {
			add<ItemListFragment>(R.id.content_view)
		}
	}

	// Forward key events to fragments
	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean = supportFragmentManager.fragments
		.filter { it.isVisible }
		.filterIsInstance<View.OnKeyListener>()
		.any { it.onKey(currentFocus, keyCode, event) }

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyDown(keyCode, event)

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)
}
