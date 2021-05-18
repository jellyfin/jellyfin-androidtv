package org.jellyfin.androidtv.ui.search

import android.R
import android.content.Intent
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import timber.log.Timber


class SearchActivity : FragmentActivity() {
	private val isSpeechEnabled by lazy {
		SpeechRecognizer.isRecognitionAvailable(this)
	}

	private fun handleIntent(intent: Intent): Boolean {
		if (Intent.ACTION_VIEW == intent.action) {
			// Handle a suggestions click (because the suggestions all use ACTION_VIEW)
			val data: String? = intent.data.toString()
			Timber.d("LAUNCH: " + data)
			val newIntent = Intent(this, StartupActivity::class.java).apply {
				putExtra(StartupActivity.EXTRA_ITEM_ID, data)
			}
			startActivity(newIntent)
			finish()
			return true
		}
		return false
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (handleIntent(intent))
			return

		// Determine fragment to use
		val searchFragment = when {
			isSpeechEnabled -> LeanbackSearchFragment()
			else -> TextSearchFragment()
		}

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.replace(R.id.content, searchFragment)
			.commit()
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)

		setIntent(intent);
		handleIntent(getIntent());
	}

	override fun onSearchRequested(): Boolean {
		// Reset layout
		recreate()

		return true
	}
}
