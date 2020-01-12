package org.jellyfin.androidtv.playback.nextup

import android.content.Intent
import android.os.Bundle
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.ImageOptions

class UpNextFragment : DetailsSupportFragment() {
	private val backgroundController = DetailsSupportFragmentBackgroundController(this)

	private suspend fun loadItem(id: String) = withContext(Dispatchers.IO) {
		val item = TvApp.getApplication().apiClient.getItem(id) ?: return@withContext null

		val backdrop = TvApp.getApplication().apiClient.GetBackdropImageUrls(item, ImageOptions()).firstOrNull()
		val thumbnail = TvApp.getApplication().apiClient.GetImageUrl(item, ImageOptions())

		UpNextItemData(
			item.id,
			"${item.parentIndexNumber}. ${item.name}",
			item.overview,
			backdrop,
			thumbnail
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		backgroundController.enableParallax()

		GlobalScope.launch(Dispatchers.Main) {
			val item = loadItem("e4f090de63eaf5fb5d34df6a7f8e504e")
//			val item = loadItem("aa170e9f519e71d17724f1c8a045c027")
//			val item = loadItem("fa78ba27f0460c1daa7622120dbdea0b")

			if (item == null) return@launch

			if (item.backdrop != null)
				backgroundController.coverBitmap = withContext(Dispatchers.IO) { Picasso.with(activity).load(item.backdrop).get() }

			adapter = ArrayObjectAdapter(ClassPresenterSelector().apply {
				addClassPresenter(DetailsOverviewRow::class.java, FullWidthDetailsOverviewRowPresenter(UpNextDetailsPresenter(activity!!)))
			}).apply {
				add(DetailsOverviewRow(item).apply {
					actionsAdapter = ArrayObjectAdapter().apply {
						add(Action(1, "Play now"))
						add(Action(1, "Go to details"))
					}
				})
			}
		}
	}

	fun play(item: BaseItemDto) {
		val intent = Intent(activity, TvApp.getApplication().getPlaybackActivityClass(item.baseItemType))
		startActivity(intent)
		activity?.finish()
	}
}
