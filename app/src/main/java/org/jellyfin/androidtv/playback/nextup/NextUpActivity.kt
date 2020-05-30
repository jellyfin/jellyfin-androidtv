package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.ImageOptions

private const val LOG_TAG = "NextUpActivity"

class NextUpActivity : FragmentActivity() {
	private lateinit var fragment: NextUpFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra("id")
		if (id == null) {
			Log.e(LOG_TAG, "No id found in bundle at onCreate().")
			finish()
			return
		}

		// Add background manager
		BackgroundManager.getInstance(this).attach(window)

		// Load item info
		GlobalScope.launch(Dispatchers.Main) {
			val data = loadItemData(id)

			if (data == null) {
				Log.e(LOG_TAG, "Unable to load data at onCreate().")
				finish()
				return@launch
			}

			// Create fragment
			fragment = NextUpFragment(data)
			supportFragmentManager
				.beginTransaction()
				.add(android.R.id.content, fragment)
				.commit()
		}
	}

	private suspend fun loadItemData(id: String) = withContext(Dispatchers.IO) {
		val item = TvApp.getApplication().apiClient.getItem(id) ?: return@withContext null

		val backdrop = TvApp.getApplication().apiClient.GetBackdropImageUrls(item, ImageOptions()).firstOrNull()
		val thumbnail = TvApp.getApplication().apiClient.GetImageUrl(item, ImageOptions())
		val logo = TvApp.getApplication().apiClient.GetLogoImageUrl(item, ImageOptions())

		val title = if (item.indexNumber != null && item.name != null)
			"${item.indexNumber}. ${item.name}"
		else if (item.name != null)
			item.name
		else ""

		NextUpItemData(
			item.id,
			title,
			item.overview,
			backdrop?.let { Glide.with(this@NextUpActivity).asBitmap().load(it).submit().get() },
			thumbnail?.let { Glide.with(this@NextUpActivity).asBitmap().load(it).submit().get() },
			logo?.let { Glide.with(this@NextUpActivity).asBitmap().load(it).submit().get() }
		)
	}
}
