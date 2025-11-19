package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.ActivityStartupBinding
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.startup.fragment.SelectServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.ServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.SplashFragment
import org.jellyfin.androidtv.ui.startup.fragment.StartupToolbarFragment
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.UUID

class StartupActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
	}

	private val startupViewModel: StartupViewModel by viewModel()
	private val api: ApiClient by inject()
	private val mediaManager: MediaManager by inject()
	private val sessionRepository: SessionRepository by inject()
	private val userRepository: UserRepository by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val itemLauncher: ItemLauncher by inject()

	private lateinit var binding: ActivityStartupBinding

	private val networkPermissionsRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { grants ->
		val anyRejected = grants.any { !it.value }

		if (anyRejected) {
			// Permission denied, exit the app.
			Toast.makeText(this, R.string.no_network_permissions, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onPermissionsGranted()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		binding = ActivityStartupBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		binding.screensaver.isVisible = false
		setContentView(binding.root)

		if (!intent.getBooleanExtra(EXTRA_HIDE_SPLASH, false)) showSplash()

		// Ensure basic permissions
		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	private fun onPermissionsGranted() = sessionRepository.state
		.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
		.filter { it == SessionRepositoryState.READY }
		.map { sessionRepository.currentSession.value }
		.distinctUntilChanged()
		.onEach { session ->
			if (session != null) {
				Timber.i("Found a session in the session repository, waiting for the currentUser in the application class.")

				showSplash()

				val currentUser = userRepository.currentUser.first { it != null }
				Timber.i("CurrentUser changed to ${currentUser?.id} while waiting for startup.")

				lifecycleScope.launch {
					openNextActivity()
				}
			} else {
				// Clear audio queue in case left over from last run
				mediaManager.clearAudioQueue()

				val server = startupViewModel.getLastServer()
				if (server != null) showServer(server.id)
				else showServerSelection()
			}
		}.launchIn(lifecycleScope)

	private suspend fun openNextActivity() {
		val itemId = when {
			intent.action == Intent.ACTION_VIEW && intent.data != null -> intent.data.toString()
			else -> intent.getStringExtra(EXTRA_ITEM_ID)
		}?.toUUIDOrNull()
		val itemIsUserView = intent.getBooleanExtra(EXTRA_ITEM_IS_USER_VIEW, false)

		Timber.i("Determining next activity (action=${intent.action}, itemId=$itemId, itemIsUserView=$itemIsUserView)")

		// Start session
		(application as? JellyfinApplication)?.onSessionStart()

		// Create destination
		val destination = when {
			// Search is requested
			intent.action === Intent.ACTION_SEARCH -> Destinations.search(
				query = intent.getStringExtra(SearchManager.QUERY)
			)
			// User view item is requested
			itemId != null && itemIsUserView -> runCatching {
				val item = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
				itemLauncher.getUserViewDestination(item)
			}.onFailure { throwable ->
				Timber.w(throwable, "Failed to retrieve item $itemId from server.")
			}.getOrNull()
			// Other item is requested
			itemId != null -> Destinations.itemDetails(itemId)
			// No destination requested, use default
			else -> null
		}

		navigationRepository.reset(destination, true)

		val intent = Intent(this, MainActivity::class.java)
		// Clear navigation history
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		Timber.i("Opening next activity $intent")
		startActivity(intent)
		finishAfterTransition()
	}

	// Fragment switching
	private fun showSplash() {
		// Prevent progress bar flashing
		if (supportFragmentManager.findFragmentById(R.id.content_view) is SplashFragment) return

		supportFragmentManager.commit {
			replace<SplashFragment>(R.id.content_view)
		}
	}

	private fun showServer(id: UUID) = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<ServerFragment>(
			R.id.content_view, null, bundleOf(
				ServerFragment.ARG_SERVER_ID to id.toString()
			)
		)
	}

	private fun showServerSelection() = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<SelectServerFragment>(R.id.content_view)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
	}
}
