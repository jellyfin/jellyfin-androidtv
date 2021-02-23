package org.jellyfin.androidtv.data.service

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment

/**
 * Fragment used to hook into the activity lifecycle and set the background when neccesary.
 */
internal class BackgroundServiceFragment(
	private val backgroundService: BackgroundService
) : Fragment() {
	private var backgrounds: Array<Drawable>? = null

	override fun onResume() {
		super.onResume()

		if (backgrounds != null) {
			backgroundService.backgrounds.clear()
			backgroundService.backgrounds.addAll(backgrounds!!)
			backgroundService.update()
		}

		activity?.window?.decorView?.apply {
			// We need to force the system to add a new callback
			// this won't happen if we set the background to the same one
			// so we set it to null first
			if (background == backgroundService.backgroundDrawable) background = null

			background = backgroundService.backgroundDrawable
		}
	}

	override fun onPause() {
		super.onPause()

		backgrounds = backgroundService.backgrounds.toTypedArray()
	}
}
