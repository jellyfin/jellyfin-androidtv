package org.jellyfin.androidtv.details.trailers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView
import org.jellyfin.androidtv.util.ImageUtils

class YouTubeTrailerPresenter(private val context: Context, private val imageHeight: Int, safeBrandingCompliance: Boolean) : Presenter(), IItemClickListener {
	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		val trailer = item as YouTubeTrailer

		val cardView = (viewHolder.view as MultiBadgeImageCardView).apply {
			isFocusable = true
			isFocusableInTouchMode = true
			setMainImageDimensions((ImageUtils.ASPECT_RATIO_16_9 * imageHeight).toInt(), imageHeight)
			mainImageDrawable = context.getDrawable(R.drawable.banner_youtube)
			titleText = trailer.name
			contentText = context.getString(R.string.brand_youtube)
		}

		// TODO: Kick off loading of thumbnail here
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {

	}

	override suspend fun onItemClicked(item: Any?) {
		if (item !is YouTubeTrailer)
			throw IllegalArgumentException("Tried to pass ExternalTrailerProvide.onClick a non-media-url")

		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.playbackURL))

		if (intent.resolveActivity(context.packageManager) != null)
			context.startActivity(intent)
		else
			Toast.makeText(context, R.string.toast_no_trailer_handler, Toast.LENGTH_LONG).show()
	}
}
