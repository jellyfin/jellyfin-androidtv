package org.jellyfin.androidtv.details

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.presentation.MultiBadgeImageCardView
import org.jellyfin.androidtv.util.dp

private const val LOG_TAG = "ItemPosterPresenter"

class ItemPosterPresenter(private val context: Context) : Presenter(), IItemClickListener {

	override suspend fun onItemClicked(item: Any?) {
		requireNotNull(item)
		val baseItem = item as BaseItem
		val intent = Intent(context, DetailsActivity::class.java)
		intent.putExtra("id", baseItem.id)

		context.startActivity(intent)
	}

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		val baseItem = item as BaseItem
		val cardView = (viewHolder.view as MultiBadgeImageCardView).apply {
			titleText = baseItem.name
			contentText = baseItem.description
			isFocusable = true
			isFocusableInTouchMode = true
			setMainImageDimensions(100.dp, 150.dp)
			mainImageDrawable = TvApp.getApplication().getDrawableCompat(R.drawable.tile_port_video)
		}


		if (baseItem.images.primary != null) {
			GlobalScope.launch(Dispatchers.Main) {
				cardView.mainImageDrawable = BitmapDrawable(baseItem.images.primary.getBitmap(TvApp.getApplication()))
			}
		}

	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
		val cardView = viewHolder!!.view as MultiBadgeImageCardView

		// TODO: Somehow release BitmapDrawable?
	}
}
