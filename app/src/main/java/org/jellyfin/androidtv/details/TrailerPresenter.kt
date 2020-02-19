package org.jellyfin.androidtv.details

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.leanback.widget.Presenter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.base.IItemClickListener
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.ui.FavoriteBadge
import org.jellyfin.androidtv.ui.MultiBadgeImageCardView
import org.jellyfin.androidtv.ui.WatchedBadge
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.dp
import org.jellyfin.apiclient.model.entities.MediaUrl

private const val SAFE_BRANDING_COMPLIANCE: Boolean = true

class TrailerPresenter(private val context: Context, private val imageHeight: Int) : Presenter(), IItemClickListener {

	val tldRegex = Regex("""^(.*\.|)(.+\...+)$""")
	val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	override suspend fun onItemClicked(item: Any?) {
		requireNotNull(item)
		if (item is BaseItem) {
			val intent = Intent(context, DetailsActivity::class.java)
			intent.putExtra("id", item.id)
			context.startActivity(intent)
		} else if (item is MediaUrl) {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))

			if (intent.resolveActivity(context.packageManager) != null)
				context.startActivity(intent)
			else
				Toast.makeText(context, R.string.toast_no_trailer_handler, Toast.LENGTH_LONG).show()
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
		return ViewHolder(MultiBadgeImageCardView(ContextThemeWrapper(parent!!.context, R.style.MarqueeImageCardViewTheme)))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {

		val cardView = (viewHolder.view as MultiBadgeImageCardView).apply {
			isFocusable = true
			isFocusableInTouchMode = true
			setMainImageDimensions((ImageUtils.ASPECT_RATIO_16_9 * imageHeight).toInt(), imageHeight)
			mainImageDrawable = TvApp.getApplication().getDrawableCompat(R.drawable.tile_port_video)
		}

		if (item is BaseItem) {
			setupBaseItemCard(cardView, item)
		} else if (item is MediaUrl) {
			setupURLCard(cardView, item.name, Uri.parse(item.url))
		} else {
			throw IllegalArgumentException("Tried to present an unsupported item as trailer")
		}
	}

	private fun isYoutubeUrl(uri: Uri) = youtubeDomains.contains(getDomain(uri))

	private fun getDomain(uri: Uri): String? = uri.host?.let { host -> tldRegex.find(host)?.groups?.get(2)?.value }

	private fun getProviderIcon(uri: Uri): Drawable? {
		return if (isYoutubeUrl(uri) && !SAFE_BRANDING_COMPLIANCE)
			context.getDrawable(R.drawable.ic_youtube)
		else
			null
	}

	private fun getThumbnailURL(uri: Uri): String? {
		if (isYoutubeUrl(uri) && uri.queryParameterNames.contains("v")) {
			return "https://img.youtube.com/vi/%s/0.jpg".format(uri.getQueryParameter("v"))
		} else {
			return null
		}
	}

	private fun setupURLCard(cardView: MultiBadgeImageCardView, name: String, uri: Uri) {
		cardView.apply {
			titleText = name

			contentText = if (isYoutubeUrl(uri)) "YouTube" else getDomain(uri)

			getProviderIcon(uri)?.let {
				val providerBadge = ImageView(context).apply {
					setImageDrawable(it)
					val params = ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
					params.setMargins(0, 0, 4.dp, 4.dp)
					layoutParams = params

				}

				setBadge(MultiBadgeImageCardView.BadgeLocation.BOTTOM_RIGHT, providerBadge)
			}
		}


		if (!SAFE_BRANDING_COMPLIANCE) {
			getThumbnailURL(uri)?.let { thumbnailURL ->
				Picasso.with(context).load(thumbnailURL).into(cardView.main_image)
			}
		}
		else if (isYoutubeUrl(uri)){
			cardView.main_image.setImageResource(R.drawable.banner_youtube)
			cardView.main_image.setBackgroundColor(context.resources.getColor(R.color.youtube_background))
		}
	}

	private fun setupBaseItemCard(cardView: MultiBadgeImageCardView, baseItem: BaseItem) {
		cardView.apply {
			titleText = baseItem.title
			contentText = context.getString(R.string.lbl_local_trailer)

			if (baseItem is PlayableItem)
				setBadge(MultiBadgeImageCardView.BadgeLocation.TOP_RIGHT, WatchedBadge(context, baseItem))

			setBadge(MultiBadgeImageCardView.BadgeLocation.BOTTOM_RIGHT, FavoriteBadge(context, baseItem))
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
