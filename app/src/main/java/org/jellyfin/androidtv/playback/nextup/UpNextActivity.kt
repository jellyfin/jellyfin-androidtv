package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.acra.ACRA.LOG_TAG
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.ImageOptions

class UpNextActivity : FragmentActivity() {
	private lateinit var fragment: UpNextFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra("id")
		if (id == null) {
			Log.e(LOG_TAG, "No id found in bundle for UpNextActivity.")
			finish()
		}

		// Add background manager
		BackgroundManager.getInstance(this).attach(window)

		// Load item info
		GlobalScope.launch(Dispatchers.Main) {
			val data = loadItemData(id) ?: return@launch

			// Create fragment
			fragment = UpNextFragment(data)
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

		val title = if (item.indexNumber != null && item.name != null)
			"${item.indexNumber}. ${item.name}"
		else if (item.name != null)
			item.name
		else ""

		UpNextItemData(
			item.id,
			title,
			item.overview,
			backdrop?.let { Picasso.with(this@UpNextActivity).load(it).get() },
			thumbnail?.let { Picasso.with(this@UpNextActivity).load(it).get() }
		)
	}
}
