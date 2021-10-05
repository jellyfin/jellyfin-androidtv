package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.fragment.app.FragmentActivity

class SearchActivity : FragmentActivity() {
	private val isSpeechEnabled by lazy {
		SpeechRecognizer.isRecognitionAvailable(this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Determine fragment to use
		val searchFragment = when {
			isSpeechEnabled -> LeanbackSearchFragment()
			else -> TextSearchFragment()
		}

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, searchFragment)
			.commit()
	}

	override fun onSearchRequested(): Boolean {
		// Reset layout
		recreate()

		return true
	}
}
