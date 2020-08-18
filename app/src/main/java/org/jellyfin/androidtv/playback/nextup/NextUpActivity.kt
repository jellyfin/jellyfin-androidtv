package org.jellyfin.androidtv.playback.nextup

import android.graphics.Bitmap
import android.os.Bundle
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
import timber.log.Timber

class NextUpActivity : FragmentActivity() {
	private lateinit var fragment: NextUpFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra("id")
		if (id == null) {
			Timber.e("No id found in bundle at onCreate().")
			finish()
			return
		}

		// Add background manager
		BackgroundManager.getInstance(this).attach(window)

		// Load item info
		GlobalScope.launch(Dispatchers.Main) {
			val data = loadItemData(id)

			if (data == null) {
				Timber.e("Unable to load data at onCreate().")
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

	private fun safelyLoadBitmap(url: String): Bitmap? = try {
		Glide.with(this).asBitmap().load(url).submit().get()
	} catch (e: Exception) {
		null
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
			backdrop?.let { safelyLoadBitmap(it) },
			thumbnail?.let { safelyLoadBitmap(it) },
			logo?.let { safelyLoadBitmap(it) }
		)
	}
}
