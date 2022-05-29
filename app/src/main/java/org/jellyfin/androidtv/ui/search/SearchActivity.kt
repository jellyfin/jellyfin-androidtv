package org.jellyfin.androidtv.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class SearchActivity : FragmentActivity() {
	private val isSpeechEnabled by lazy {
		SpeechRecognizer.isRecognitionAvailable(this)
			&& ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED
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
