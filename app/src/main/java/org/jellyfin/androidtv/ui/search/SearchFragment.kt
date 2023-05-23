package org.jellyfin.androidtv.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.R

class SearchFragment : Fragment(R.layout.fragment_content_view) {
	private val isSpeechEnabled by lazy {
		SpeechRecognizer.isRecognitionAvailable(requireContext())
			&& ContextCompat.checkSelfPermission(
			requireContext(),
			Manifest.permission.RECORD_AUDIO
		) != PackageManager.PERMISSION_DENIED
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Determine fragment to use
		val searchFragment = when {
			isSpeechEnabled -> LeanbackSearchFragment()
			else -> TextSearchFragment()
		}

		// Add fragment
		childFragmentManager
			.beginTransaction()
			.replace(R.id.content_view, searchFragment)
			.commit()
	}
}
