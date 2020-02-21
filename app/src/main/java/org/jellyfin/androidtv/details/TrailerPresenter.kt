package org.jellyfin.androidtv.details

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.details.trailerprovider.LocalTrailerProvider
import org.jellyfin.androidtv.details.trailerprovider.YouTubeProvider
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.dp

class TrailerPresenter(private val context: Context, private val imageHeight: Int) : Presenter(), IItemClickListener {

	// Queried from the front, first one that can handle an item gets to do it.
	private val trailerProviders = arrayOf(YouTubeProvider(context, true), LocalTrailerProvider(context))

	override suspend fun onItemClicked(item: Any?) {
		requireNotNull(item)
		trailerProviders.firstOrNull { it.canHandle(item) }?.onClick(item)
	}

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		val responsibleProvider = trailerProviders.first { it.canHandle(item) }

		val cardView = (viewHolder.view as MultiBadgeImageCardView).apply {
			isFocusable = true
			isFocusableInTouchMode = true
			setMainImageDimensions((ImageUtils.ASPECT_RATIO_16_9 * imageHeight).toInt(), imageHeight)
			mainImageDrawable = responsibleProvider.getPlaceholder()
			titleText = responsibleProvider.getName(item)
			contentText = responsibleProvider.getDescription(item)
		}

		responsibleProvider.loadThumbnail(item) {
			if (responsibleProvider.getIcon() != null) {
				val providerBadge = ImageView(context).apply {
					setImageDrawable(responsibleProvider.getIcon())
					val params = ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
					params.setMargins(0, 0, 4.dp, 4.dp)
					layoutParams = params

				}

				cardView.setBadge(MultiBadgeImageCardView.BadgeLocation.BOTTOM_RIGHT, providerBadge)
			}

			cardView.main_image.setImageDrawable(it)
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
		val cardView = viewHolder!!.view as MultiBadgeImageCardView

		// TODO: Somehow release BitmapDrawable?
	}
}
