package org.jellyfin.androidtv.details

import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Person
import org.jellyfin.androidtv.presentation.MyImageCardView

private const val LOG_TAG = "PersonPresenter"

class PersonPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(ImageCardView(parent!!.context))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
		requireNotNull(item) { "Tried to bind null" }
		val person = item as Person
		val cardView = viewHolder!!.view as ImageCardView

		cardView.titleText = person.name
		cardView.contentText = person.role
		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true
		cardView.setMainImageDimensions(240, 300)
		cardView.mainImage = TvApp.getApplication().getDrawableCompat(R.drawable.tile_port_person)

		if (person.primaryImage != null) {
			GlobalScope.launch(Dispatchers.Main) {
				cardView.mainImage = BitmapDrawable(person.primaryImage.getBitmap(TvApp.getApplication()))
			}
		}

	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
		val cardView = viewHolder!!.view as ImageCardView

		// TODO: Somehow release BitmapDrawable?
	}
}
