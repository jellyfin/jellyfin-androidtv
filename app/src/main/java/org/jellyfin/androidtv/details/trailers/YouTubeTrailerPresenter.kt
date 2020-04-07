package org.jellyfin.androidtv.details.trailers

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView

class YouTubeTrailerPresenter(
	private val context: Context,
	imageHeight: Int
) : ExternalTrailerPresenter(context, imageHeight) {

	override val description = context.getString(R.string.domain_youtube)

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		super.onBindViewHolder(viewHolder, item)

		val trailer = item as YouTubeTrailer
		val cardView = (viewHolder.view as MultiBadgeImageCardView)

		Glide.with(context).load(trailer.thumbnailURL).into(cardView.main_image)
	}
}
