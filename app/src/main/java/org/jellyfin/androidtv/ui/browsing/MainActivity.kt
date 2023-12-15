package org.jellyfin.androidtv.ui.browsing

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.ActivityMainBinding
import org.jellyfin.androidtv.ui.ScreensaverViewModel
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.navigation.NavigationAction
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.screensaver.InAppScreensaver
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.applyTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : FragmentActivity() {
	companion object {
		private const val FRAGMENT_TAG_CONTENT = "content"
	}

	private val navigationRepository by inject<NavigationRepository>()
	private val sessionRepository by inject<SessionRepository>()
	private val userRepository by inject<UserRepository>()
	private val screensaverViewModel by viewModel<ScreensaverViewModel>()
	private var inTransaction = false

	private lateinit var binding: ActivityMainBinding

	private val backPressedCallback = object : OnBackPressedCallback(false) {
		override fun handleOnBackPressed() {
			if (navigationRepository.canGoBack) navigationRepository.goBack()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		if (!validateAuthentication()) return

		screensaverViewModel.keepScreenOn.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { keepScreenOn ->
				if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}.launchIn(lifecycleScope)

		onBackPressedDispatcher.addCallback(this, backPressedCallback)

		supportFragmentManager.addOnBackStackChangedListener {
			if (!inTransaction && supportFragmentManager.backStackEntryCount == 0)
				navigationRepository.reset()
		}

		if (savedInstanceState == null && navigationRepository.canGoBack) navigationRepository.reset()

		navigationRepository.currentAction
			.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { action ->
				handleNavigationAction(action)
				backPressedCallback.isEnabled = navigationRepository.canGoBack
				screensaverViewModel.notifyInteraction(false)
			}.launchIn(lifecycleScope)

		binding = ActivityMainBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		binding.screensaver.setContent { InAppScreensaver() }
		setContentView(binding.root)
	}

	override fun onResume() {
		super.onResume()

		if (!validateAuthentication()) return

		applyTheme()

		screensaverViewModel.activityPaused = false
	}

	private fun validateAuthentication(): Boolean {
		if (sessionRepository.currentSession.value == null || userRepository.currentUser.value == null) {
			Timber.w("Activity ${this::class.qualifiedName} started without a session, bouncing to StartupActivity")
			startActivity(Intent(this, StartupActivity::class.java))
			finish()
			return false
		}

		return true
	}

	override fun onPause() {
		super.onPause()

		screensaverViewModel.activityPaused = true
	}

	override fun onStop() {
		super.onStop()

		lifecycleScope.launch {
			Timber.d("MainActivity stopped")
			sessionRepository.restoreSession(destroyOnly = true)
		}
	}

	private fun handleNavigationAction(action: NavigationAction) = when (action) {
		is NavigationAction.NavigateFragment -> {
			if (action.replace) {
				// Prevent back stack changed listener from resetting when popping to
				// the initial destination
				inTransaction = true
				supportFragmentManager.popBackStack()
				inTransaction = false
			}

			supportFragmentManager.commit {
				val destination = action.destination
				val currentFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_CONTENT)
				val isSameFragment = currentFragment != null &&
					destination.fragment.isInstance(currentFragment) &&
					currentFragment.arguments == destination.arguments

				if (!isSameFragment) {
					setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)

					replace(R.id.content_view, destination.fragment.java, destination.arguments, FRAGMENT_TAG_CONTENT)
				}

				if (action.addToBackStack) addToBackStack(null)
			}
		}

		is NavigationAction.NavigateActivity -> {
			val destination = action.destination
			val intent = Intent(this@MainActivity, destination.activity.java)
			intent.putExtras(destination.extras)
			startActivity(intent)
			action.onOpened()
		}

		NavigationAction.GoBack -> supportFragmentManager.popBackStack()

		NavigationAction.Nothing -> Unit
	}

	// Forward key events to fragments
	private fun Fragment.onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		var result = childFragmentManager.fragments.any { it.onKeyEvent(keyCode, event) }
		if (!result && this is View.OnKeyListener) result = onKey(currentFocus, keyCode, event)
		return result
	}

	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		// Ignore the key event that closes the screensaver
		if (screensaverViewModel.visible.value) {
			screensaverViewModel.notifyInteraction(canCancel = event?.action == KeyEvent.ACTION_UP)
			return true
		}

		return supportFragmentManager.fragments
			.any { it.onKeyEvent(keyCode, event) }
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyDown(keyCode, event)

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)

	override fun onUserInteraction() {
		super.onUserInteraction()

		screensaverViewModel.notifyInteraction(false)
	}
}
