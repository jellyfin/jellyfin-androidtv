package org.jellyfin.androidtv.ui.details

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_expanded_text.*
import org.jellyfin.androidtv.R

class ExpandedTextActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_expanded_text)

		expanded_text_content.transitionName = TRANSITION_NAME
		expanded_text_content.text = intent.getStringExtra(EXTRA_TEXT)
	}

	companion object {
		const val EXTRA_TEXT = "text"
		const val TRANSITION_NAME = "expanded_text_content"
	}
}
