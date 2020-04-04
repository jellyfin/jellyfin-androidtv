package org.jellyfin.androidtv.details.trailers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView
import org.jellyfin.androidtv.util.ImageUtils

class YouTubeTrailerPresenter(private val context: Context,
							  private val imageHeight: Int,
							  private val safeBrandingCompliance: Boolean
) : Presenter(), IItemClickListener {
	private val LOG_TAG = "YouTubeTrailerPresenter"

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

		if (!safeBrandingCompliance) {
			GlobalScope.launch(Dispatchers.IO) {
				Glide.with(context).load(trailer.thumbnailURL)
			}

			Glide.with(context).load(trailer.thumbnailURL).listener(object : RequestListener<String, GlideDrawable> {
				override fun onException(e: Exception, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
					Log.e(LOG_TAG, "Failed to load thumbnail: $e")
					return false
				}

				override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
					val badge = ImageView(context)
					badge.setImageDrawable(context.getDrawable(R.drawable.ic_youtube))

					cardView.setBadge(MultiBadgeImageCardView.BadgeLocation.BOTTOM_RIGHT, badge)
					return false
				}

			}).into(cardView.main_image)
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		(viewHolder.view as MultiBadgeImageCardView).apply {
			setBadge(MultiBadgeImageCardView.BadgeLocation.BOTTOM_RIGHT, null)
		}
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
