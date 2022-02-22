package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.search.SearchActivity
import org.jellyfin.androidtv.ui.startup.fragment.SelectServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.ServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.SplashFragment
import org.jellyfin.androidtv.ui.startup.fragment.StartupToolbarFragment
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StartupActivity : FragmentActivity(R.layout.fragment_content_view) {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
	}

	private val loginViewModel: LoginViewModel by viewModel()
	private val apiClient: ApiClient by inject()
	private val mediaManager: MediaManager by inject()
	private val serverRepository: ServerRepository by inject()
	private val sessionRepository: SessionRepository by inject()

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
		super.onCreate(savedInstanceState)

		if (!intent.getBooleanExtra(EXTRA_HIDE_SPLASH, false)) showSplash()

		// Migrate old credentials
		runBlocking {
			serverRepository.migrateLegacyCredentials()
		}

		// Ensure basic permissions
		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
	}

	private fun onPermissionsGranted() {
		var isLoaded = false

		sessionRepository.currentSession.observe(this) { session ->
			if (session != null) {
				Timber.i("Found a session in the session repository, waiting for the currentUser in the application class.")

				showSplash()

				(application as? TvApp)?.currentUserLiveData?.observe(this) { currentUser ->
					Timber.i("CurrentUser changed to ${currentUser?.id} while waiting for startup.")

					if (currentUser != null) lifecycleScope.launch {
						openNextActivity()
					}
				}
			} else if (!isLoaded) {
				// Clear audio queue in case left over from last run
				mediaManager.clearAudioQueue()
				mediaManager.clearVideoQueue()

				val server = loginViewModel.getLastServer()
				if (server != null) showServer(server.id)
				else showServerSelection()

				isLoaded = true
			}
		}
	}

	private suspend fun openNextActivity() {
		val itemId = when {
			intent.action == Intent.ACTION_VIEW && intent.data != null -> intent.data.toString()
			else -> intent.getStringExtra(EXTRA_ITEM_ID)
		}
		val itemIsUserView = intent.getBooleanExtra(EXTRA_ITEM_IS_USER_VIEW, false)

		Timber.d("Determining next activity (action=${intent.action}, itemId=$itemId, itemIsUserView=$itemIsUserView)")

		// Start session
		(application as? JellyfinApplication)?.onSessionStart()

		// Create intent
		val intent = when {
			// Search is requested
			intent.action === Intent.ACTION_SEARCH -> {
				Intent(this, SearchActivity::class.java).apply {
					action = intent.action
					data = intent.data
					putExtras(intent)
				}
			}
			// Item is requested
			itemId != null -> when {
				// Item is a user view - need to get info from API and create the intent
				// using the ItemLauncher
				itemIsUserView -> callApi<BaseItemDto?> {
					apiClient.GetItemAsync(itemId, apiClient.currentUserId, it)
				}?.let { item ->
					suspendCoroutine { continuation ->
						ItemLauncher.createUserViewIntent(item, this) { intent ->
							continuation.resume(intent)
						}
					}
				}
				// Item is not a user view
				else -> Intent(this, FullDetailsActivity::class.java).apply {
					putExtra(EXTRA_ITEM_ID, itemId)
				}
			}
			// Launch default
			else -> null
		} ?: Intent(this, MainActivity::class.java)

		// Clear navigation history
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		Timber.d("Opening next activity $intent")
		startActivity(intent)
		finishAfterTransition()
	}

	// Fragment switching
	private fun showSplash() = supportFragmentManager.commit {
		replace<SplashFragment>(R.id.content_view)
	}

	private fun showServer(id: UUID) = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<ServerFragment>(R.id.content_view, null, bundleOf(
			ServerFragment.ARG_SERVER_ID to id.toString()
		))
	}

	private fun showServerSelection() = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<SelectServerFragment>(R.id.content_view)
	}
}
