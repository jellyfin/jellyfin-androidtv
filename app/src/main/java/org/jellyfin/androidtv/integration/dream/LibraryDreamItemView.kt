package org.jellyfin.androidtv.integration.dream

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import org.jellyfin.androidtv.databinding.ViewDreamLibraryItemBinding
import org.jellyfin.androidtv.util.KenBurnsTransitionGenerator

class LibraryDreamItemView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewDreamLibraryItemBinding.inflate(LayoutInflater.from(context), this, true)

	private val kenburnsTransitionGenerator = KenBurnsTransitionGenerator(
		LibraryDreamService.UPDATE_DELAY,
		AccelerateDecelerateInterpolator()
	)

	init {
		binding.background.setTransitionGenerator(kenburnsTransitionGenerator)
	}

	fun setItem(item: LibraryDreamItem) {
		kenburnsTransitionGenerator.reset()

		binding.background.setImageDrawable(item.background)
		binding.title.text = item.baseItem.name
	}
}
