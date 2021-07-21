package org.jellyfin.androidtv.data.service

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Fragment used to hook into the activity lifecycle and set the background when neccesary.
 */
internal class BackgroundServiceFragment : Fragment() {
	private val backgroundService by inject<BackgroundService>()
	private var backgrounds: Array<Drawable>? = null

	override fun onResume() {
		super.onResume()

		if (backgrounds != null) {
			Timber.d("Restoring active backgrounds")

			backgroundService.backgrounds.clear()
			backgroundService.backgrounds.addAll(backgrounds!!)
			backgroundService.update()
		}

		activity?.window?.decorView?.apply {
			Timber.d("Restoring background drawable")

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
