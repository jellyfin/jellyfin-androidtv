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

private const val LOG_TAG = "DetailsActivity"

class DetailsActivity : FragmentActivity() {
	private lateinit var fragment: BaseDetailsFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra("id")
		if (id == null) {
			Log.e(LOG_TAG, "No id was passed to Details Activity, closing automatically again.")
			finish()
		}

		GlobalScope.launch(Dispatchers.Main) {
			val baseItem = getBaseItemDtoForID(id) ?: return@launch

			fragment = when (baseItem.baseItemType!!) {
				BaseItemType.Episode -> {
					val episode = baseItem.asEpisode()
					EpisodeDetailsFragment(episode)
				}
				else -> TODO()
			}

			supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
		}
	}

	private suspend fun getBaseItemDtoForID(id: String) = withContext(Dispatchers.IO) {
		TvApp.getApplication().apiClient.getItem(id)
	}
}
