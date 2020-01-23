package org.jellyfin.androidtv.details

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.asEpisode
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ImageOptions

class DetailsActivity : FragmentActivity() {
	private lateinit var fragment: BaseDetailsFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra("id")
		if (id == null) finish()

		BackgroundManager.getInstance(this).attach(window)

		GlobalScope.launch(Dispatchers.Main) {
			val baseItem = getBaseItemDtoForID(id) ?: return@launch

			when (baseItem.baseItemType!!) {
				BaseItemType.Episode -> {
					val primaryImageUrl = TvApp.getApplication().apiClient.GetImageUrl(baseItem, ImageOptions())
					val primaryImageBitmap = getImageFromURL(primaryImageUrl)
					val episode = baseItem.asEpisode(primaryImageBitmap)

					fragment = EpisodeDetailsFragment(episode)
				}
				else -> TODO()
			}

			supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
		}
	}

	private suspend fun getBaseItemDtoForID(id: String) = withContext(Dispatchers.IO) {
		return@withContext TvApp.getApplication().apiClient.getItem(id)
	}

	private suspend fun getImageFromURL(url: String) = withContext(Dispatchers.IO) {
		return@withContext Picasso.with(this@DetailsActivity).load(url).get()
	}
}
