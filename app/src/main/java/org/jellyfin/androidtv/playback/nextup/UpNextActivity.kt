package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.ImageOptions

class UpNextActivity : FragmentActivity() {
	private lateinit var fragment: UpNextFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		BackgroundManager.getInstance(this).attach(window)

		GlobalScope.launch(Dispatchers.Main) {
//			val data = loadItemData("e4f090de63eaf5fb5d34df6a7f8e504e") ?: return@launch
//			val data = loadItemData("aa170e9f519e71d17724f1c8a045c027") ?: return@launch
			val data = loadItemData("fa78ba27f0460c1daa7622120dbdea0b") ?: return@launch

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

		//todo improve "title" logic
		val title = if (item.parentIndexNumber != null && item.name != null)
			"${item.parentIndexNumber}. ${item.name}"
		else if (item.name != null)
			item.name
		else if (item.parentIndexNumber != null)
			"Episode" + item.parentIndexNumber
		else ""

		UpNextItemData(
			item.id,
			title,
			item.overview,
			backdrop?.let { Picasso.with(this@UpNextActivity).load(it).get() },
			thumbnail?.let { Picasso.with(this@UpNextActivity).load(it).get() }
		)
	}

	override fun onBackPressed() {
		if (fragment.isCountdownActive()) {
			fragment.stopCountdown()
		} else {
			super.onBackPressed()
		}
	}
}
