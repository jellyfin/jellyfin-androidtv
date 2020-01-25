package org.jellyfin.androidtv.details

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.model.itemtypes.IBaseItemVisitor
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.androidtv.util.apiclient.liftToNewFormat

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
			val lifted = baseItem.liftToNewFormat()

			lifted.acceptVisitor(object : IBaseItemVisitor {
				override fun visit(item: Episode) {
					fragment = EpisodeDetailsFragment(item)
				}
			})

			supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commit()
		}
	}

	private suspend fun getBaseItemDtoForID(id: String) = withContext(Dispatchers.IO) {
		TvApp.getApplication().apiClient.getItem(id)
	}
}
