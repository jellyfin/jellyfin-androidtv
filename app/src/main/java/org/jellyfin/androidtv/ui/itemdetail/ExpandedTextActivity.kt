package org.jellyfin.androidtv.ui.itemdetail

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R

class ExpandedTextActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_expanded_text)

		findViewById<TextView>(R.id.expanded_text_content).apply {
			transitionName = TRANSITION_NAME
			text = intent.getStringExtra(EXTRA_TEXT)
		}
	}

	companion object {
		const val EXTRA_TEXT = "text"
		const val TRANSITION_NAME = "expanded_text_content"
	}
}
