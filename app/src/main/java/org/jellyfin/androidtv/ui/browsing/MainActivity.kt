package org.jellyfin.androidtv.ui.browsing

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.ActivityMainBinding
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.navigation.NavigationAction
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.screensaver.InAppScreensaver
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.androidtv.util.isMediaSessionKeyEvent
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : FragmentActivity() {
	private val navigationRepository by inject<NavigationRepository>()
	private val sessionRepository by inject<SessionRepository>()
	private val userRepository by inject<UserRepository>()
	private val interactionTrackerViewModel by viewModel<InteractionTrackerViewModel>()
	private val workManager by inject<WorkManager>()

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

		interactionTrackerViewModel.keepScreenOn.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { keepScreenOn ->
				if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}.launchIn(lifecycleScope)

		onBackPressedDispatcher.addCallback(this, backPressedCallback)
		if (savedInstanceState == null && navigationRepository.canGoBack) navigationRepository.reset(clearHistory = true)

		navigationRepository.currentAction
			.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
			.onEach { action ->
				handleNavigationAction(action)
				backPressedCallback.isEnabled = navigationRepository.canGoBack
				interactionTrackerViewModel.notifyInteraction(canCancel = false, userInitiated = false)
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

		interactionTrackerViewModel.activityPaused = false
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

		interactionTrackerViewModel.activityPaused = true
	}

	override fun onStop() {
		super.onStop()

		workManager.enqueue(OneTimeWorkRequestBuilder<LeanbackChannelWorker>().build())

		lifecycleScope.launch(Dispatchers.IO) {
			Timber.i("MainActivity stopped")
			sessionRepository.restoreSession(destroyOnly = true)
		}
	}

	private fun handleNavigationAction(action: NavigationAction) {
		interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)

		when (action) {
			is NavigationAction.NavigateFragment -> binding.contentView.navigate(action)
			NavigationAction.GoBack -> binding.contentView.goBack()

			NavigationAction.Nothing -> Unit
		}
	}

	// Forward key events to fragments
	private fun Fragment.onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		var result = childFragmentManager.fragments.any { it.onKeyEvent(keyCode, event) }
		if (!result && this is View.OnKeyListener) result = onKey(currentFocus, keyCode, event)
		return result
	}

	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		// Ignore the key event that closes the screensaver
		if (interactionTrackerViewModel.visible.value) {
			interactionTrackerViewModel.notifyInteraction(canCancel = event?.action == KeyEvent.ACTION_UP, userInitiated = true)
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

		interactionTrackerViewModel.notifyInteraction(false, userInitiated = true)
	}

	@Suppress("RestrictedApi") // False positive
	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		// Ignore the key event that closes the screensaver
		if (!event.isMediaSessionKeyEvent() && interactionTrackerViewModel.visible.value) {
			interactionTrackerViewModel.notifyInteraction(canCancel = event.action == KeyEvent.ACTION_UP, userInitiated = true)
			return true
		}

		@Suppress("RestrictedApi") // False positive
		return super.dispatchKeyEvent(event)
	}

	@Suppress("RestrictedApi") // False positive
	override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean {
		// Ignore the key event that closes the screensaver
		if (!event.isMediaSessionKeyEvent() && interactionTrackerViewModel.visible.value) {
			interactionTrackerViewModel.notifyInteraction(canCancel = event.action == KeyEvent.ACTION_UP, userInitiated = true)
			return true
		}

		@Suppress("RestrictedApi") // False positive
		return super.dispatchKeyShortcutEvent(event)
	}

	override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
		// Ignore the touch event that closes the screensaver
		if (interactionTrackerViewModel.visible.value) {
			interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = true)
			return true
		}

		return super.dispatchTouchEvent(ev)
	}
}
