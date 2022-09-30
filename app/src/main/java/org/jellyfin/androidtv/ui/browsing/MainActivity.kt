package org.jellyfin.androidtv.ui.browsing

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity(R.layout.fragment_content_view) {
	companion object {
		private const val FRAGMENT_TAG_CONTENT = "content"
	}

	private val backgroundService by inject<BackgroundService>()
	private val navigationRepository by inject<NavigationRepository>()

	private val backPressedCallback = object : OnBackPressedCallback(false) {
		override fun handleOnBackPressed() {
			if (navigationRepository.canGoBack) navigationRepository.goBack()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		backgroundService.attach(this)
		onBackPressedDispatcher.addCallback(this, backPressedCallback)

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				navigationRepository.currentDestination.collect { destination ->
					updateDestination(destination)
					backPressedCallback.isEnabled = navigationRepository.canGoBack
				}
			}
		}
	}

	private fun updateDestination(destination: Destination) {
		when (destination) {
			is Destination.Fragment -> supportFragmentManager.commit {
				val currentFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTENT)
				val isSameFragment = currentFragment != null &&
					destination.fragment.isInstance(currentFragment) &&
					currentFragment.arguments == destination.arguments

				if (!isSameFragment) {
					if (currentFragment != null) remove(currentFragment)
					add(R.id.content_view, destination.fragment.java, destination.arguments, FRAGMENT_TAG_CONTENT)
				}
			}

			is Destination.Activity -> {
				val intent = Intent(this@MainActivity, destination.activity.java)
				intent.putExtras(destination.extras)
				startActivity(intent)

				// Always pop after starting an activity to prevent it from reopening
				// because the last destination is always opened when resuming MainActivity
				navigationRepository.goBack()
			}
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
