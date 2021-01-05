package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemdetail.FullDetailsActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.koin.android.ext.android.inject
import timber.log.Timber

class StartupActivity : FragmentActivity() {
	companion object {
		private const val NETWORK_PERMISSION = 1
		const val ITEM_ID = "ItemId"
		const val ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val HIDE_SPLASH = "HideSplash"
	}

	private var application: TvApp? = null
	private val apiClient: ApiClient by inject()
	private var isLoaded = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.fragment_content_view)
		if (!intent.getBooleanExtra(HIDE_SPLASH, false)) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.content_view, SplashFragment())
				.commit()
		}
		application = applicationContext as TvApp

		//Ensure basic permissions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
			Timber.i("Requesting network permissions")
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET), NETWORK_PERMISSION)
		} else {
			Timber.i("Basic network permissions are granted")
			start()
		}
	}

	private fun start() {
		if (application!!.currentUser != null && MediaManager.isPlayingAudio()) {
			openNextActivity()
		} else {
			//clear audio queue in case left over from last run
			MediaManager.clearAudioQueue()
			MediaManager.clearVideoQueue()
			showServerList()
		}
		isLoaded = true
	}

	fun openNextActivity() {
		val itemId = intent.getStringExtra(ITEM_ID)
		val itemIsUserView = intent.getBooleanExtra(ITEM_IS_USER_VIEW, false)
		if (itemId != null) {
			if (itemIsUserView) {
				apiClient.GetItemAsync(itemId, apiClient.currentUserId, object : Response<BaseItemDto?>() {
					override fun onResponse(item: BaseItemDto?) {
						ItemLauncher.launchUserView(item, this@StartupActivity, true)
					}

					override fun onError(exception: Exception) {
						// go straight into last connection
						val intent = Intent(application, MainActivity::class.java)
						startActivity(intent)
						finish()
					}
				})
			} else {
				//Can just go right into details
				val detailsIntent = Intent(this, FullDetailsActivity::class.java)
				detailsIntent.putExtra(ITEM_ID, intent.getStringExtra(ITEM_ID))
				startActivity(detailsIntent)
				finish()
			}
		} else {
			// go straight into last connection
			val intent = Intent(this, MainActivity::class.java)
			startActivity(intent)
			finish()
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		if (requestCode == NETWORK_PERMISSION) { // If request is cancelled, the result arrays are empty.
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// permission was granted
				start()
			} else {
				// permission denied! Disable the app.
				Utils.showToast(this, "Application cannot continue without network")
				finish()
			}
		}
	}

	fun addServer() {
		supportFragmentManager.beginTransaction()
			.addToBackStack(null)
			.replace(R.id.content_view, AddServerFragment(
				onServerAdded = { id -> },
				onClose = { supportFragmentManager.popBackStack() }
			))
			.commit()
	}

	private fun showServerList() {
		supportFragmentManager.beginTransaction()
			.replace(R.id.content_view, StartupToolbarFragment())
			.add(R.id.content_view, ListServerFragment())
			.commit()
	}
}
