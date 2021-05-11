package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.koin.android.ext.android.inject
import timber.log.Timber

class StartupActivity : FragmentActivity(R.layout.fragment_content_view) {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
	}

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
			observeSession()
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

	private fun observeSession() {
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
			} else if (isLoaded == false) {
				// Clear audio queue in case left over from last run
				mediaManager.clearAudioQueue()
				mediaManager.clearVideoQueue()
				showServerList()

				isLoaded = true
			}
		}
	}

	private suspend fun openNextActivity() {
		val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
		val itemIsUserView = intent.getBooleanExtra(EXTRA_ITEM_IS_USER_VIEW, false)

		if (itemId != null) {
			if (itemIsUserView) {
				// Try opening the user view
				val item = callApi<BaseItemDto?> { apiClient.GetItemAsync(itemId, apiClient.currentUserId, it) }

				if (item != null) {
					ItemLauncher.launchUserView(item, this, true)
					finish()
					return
				}
			} else {
				// Open item details
				val detailsIntent = Intent(this, FullDetailsActivity::class.java).apply {
					putExtra(EXTRA_ITEM_ID, itemId)
				}

				startActivity(detailsIntent)
				finishAfterTransition()
				return
			}
		}

		// Go to home screen
		val intent = Intent(this, MainActivity::class.java)
		startActivity(intent)
		finishAfterTransition()
	}

	// Fragment switching

	fun showSplash() = supportFragmentManager.commit {
		replace<SplashFragment>(R.id.content_view)
	}

	fun showAddServer() = supportFragmentManager.commit {
		addToBackStack(null)
		replace<AddServerAlertFragment>(R.id.content_view)
	}

	fun showServerList() = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<OverviewFragment>(R.id.content_view)
	}
}
