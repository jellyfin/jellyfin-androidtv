package org.jellyfin.androidtv.details

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.Trailer
import org.jellyfin.androidtv.model.itemtypes.Video
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.androidtv.util.apiclient.liftToNewFormat

private const val LOG_TAG = "DetailsActivity"

class DetailsActivity : FragmentActivity() {
	private lateinit var fragment: Fragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val id = intent.getStringExtra(EXTRA_ITEM_ID)
		if (id == null) {
			Log.e(LOG_TAG, "No id was passed to Details Activity, closing automatically again.")
			finish()
			return
		}

		Log.i(LOG_TAG, "Opening item with id $id")

		GlobalScope.launch(Dispatchers.Main) {
			val baseItem = getBaseItemDtoForID(id) ?: return@launch
			Log.i(LOG_TAG, "Item ($id) type is ${baseItem.baseItemType}")

			val item = baseItem.liftToNewFormat()

			fragment = when (item) {
				is Movie -> MovieDetailsFragment(item)
				is Episode -> TODO("Episode details are not yet implemented")
				is Video -> TODO("Video details are not yet implemented")
				is Trailer -> TODO("Trailer details are not yet implemented")
			}

			supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
		}
	}

	private suspend fun getBaseItemDtoForID(id: String) = withContext(Dispatchers.IO) {
		TvApp.getApplication().apiClient.getItem(id)
	}

	companion object {
		const val EXTRA_ITEM_ID = "id"
	}
}
