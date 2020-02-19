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
import org.jellyfin.androidtv.util.dp

private const val LOG_TAG = "PersonPresenter"

class PersonPresenter(private val context: Context) : Presenter(), IItemClickListener {

	override suspend fun onItemClicked(item: Any?) {
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
		cardView.setMainImageDimensions(100.dp, 150.dp)
		cardView.mainImage = TvApp.getApplication().getDrawableCompat(R.drawable.tile_port_person)

		if (person.primaryImage != null) {
			GlobalScope.launch(Dispatchers.IO) {
				val drawable =  BitmapDrawable(person.primaryImage.getBitmap(TvApp.getApplication(), 100.dp, 150.dp))
				drawable.bitmap.prepareToDraw()
				GlobalScope.launch(Dispatchers.Main) {
					cardView.mainImage = drawable
				}
			}
		}

	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
		val cardView = viewHolder!!.view as ImageCardView

		// TODO: Somehow release BitmapDrawable?
	}
}
