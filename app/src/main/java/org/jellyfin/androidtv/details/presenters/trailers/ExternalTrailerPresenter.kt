package org.jellyfin.androidtv.details.presenters.trailers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView
import org.jellyfin.androidtv.util.ImageUtils

open class ExternalTrailerPresenter(private val context: Context, private val imageHeight: Int) : Presenter(), IItemClickListener {
	protected open val thumbnail = context.getDrawable(R.drawable.tile_chapter)
	protected open val description: String? = null

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		val trailer = item as ExternalTrailer

		(viewHolder.view as MultiBadgeImageCardView).apply {
			isFocusable = true
			isFocusableInTouchMode = true
			setMainImageDimensions((ImageUtils.ASPECT_RATIO_16_9 * imageHeight).toInt(), imageHeight)
			mainImageDrawable = thumbnail
			titleText = trailer.name
			contentText = description
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		// do nothing
	}

	override suspend fun onItemClicked(item: Any?) {
		if (item !is ExternalTrailer)
			throw IllegalArgumentException("Tried to pass ExternalTrailer.onItemClicked an item that is not a trailer")

		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.playbackURL))

		if (intent.resolveActivity(context.packageManager) != null)
			context.startActivity(intent)
		else
			Toast.makeText(context, R.string.toast_no_trailer_handler, Toast.LENGTH_LONG).show()
	}
}
