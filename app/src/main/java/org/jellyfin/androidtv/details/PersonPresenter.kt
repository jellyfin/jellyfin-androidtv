package org.jellyfin.androidtv.details

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.itemtypes.BriefPersonData

private const val LOG_TAG = "PersonPresenter"

class PersonPresenter(private val context: Context) : Presenter(), IItemClickListener {

	override fun onItemClicked(item: Any?) {
		requireNotNull(item)
		val person = item as BriefPersonData
		val intent = Intent(context, FullDetailsActivity::class.java)
		intent.putExtra("ItemId", person.id)

		context.startActivity(intent)
	}

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(ImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		val person = item as BriefPersonData
		val cardView = viewHolder.view as ImageCardView

		cardView.titleText = person.name
		cardView.contentText = person.role
		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true
		cardView.setMainImageDimensions(200, 300)
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
