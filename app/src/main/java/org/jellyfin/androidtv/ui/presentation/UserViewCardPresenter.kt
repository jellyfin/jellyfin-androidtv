package org.jellyfin.androidtv.ui.presentation

import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.card.LegacyImageCardView
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.ImageType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserViewCardPresenter(
	val small: Boolean,
) : Presenter(), KoinComponent {
	private val imageHelper by inject<ImageHelper>()

	inner class ViewHolder(
		private val cardView: LegacyImageCardView,
	) : Presenter.ViewHolder(cardView) {
		fun setItem(rowItem: BaseRowItem?) {
			val baseItem = rowItem?.baseItem

			// Load image
			val imageTag = baseItem?.imageTags?.get(ImageType.PRIMARY)
			val imageBlurhash = imageTag?.let { baseItem.imageBlurHashes?.get(ImageType.PRIMARY)?.get(it) }
			val imageUrl = imageTag?.let { imageHelper.getImageUrl(baseItem.id, ImageType.PRIMARY, it) }
			cardView.mainImageView.load(
				url = imageUrl,
				blurHash = imageBlurhash,
				placeholder = ContextCompat.getDrawable(cardView.context, R.drawable.tile_land_folder),
				aspectRatio = ImageHelper.ASPECT_RATIO_16_9,
				blurHashResolution = 32,
			)

			// Set title
			cardView.setTitleText(rowItem?.getName(cardView.context))

			// Set size
			if (small) {
				cardView.setMainImageDimensions(133, 75)
			} else {
				cardView.setMainImageDimensions(224, 126)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val cardView = LegacyImageCardView(parent.context, true)
		cardView.isFocusable = true
		cardView.isFocusableInTouchMode = true

		val typedValue = TypedValue()
		val theme = parent.context.theme
		theme.resolveAttribute(R.attr.cardViewBackground, typedValue, true)
		@ColorInt val color = typedValue.data
		cardView.setBackgroundColor(color)

		return ViewHolder(cardView)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
		if (item !is BaseRowItem) return

		(viewHolder as? ViewHolder)?.setItem(item)
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {
		(viewHolder as? ViewHolder)?.setItem(null)
	}
}
