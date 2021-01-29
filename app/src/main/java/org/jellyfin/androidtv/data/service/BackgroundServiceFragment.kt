package org.jellyfin.androidtv.data.service

import androidx.fragment.app.Fragment

/**
 * Fragment used to hook into the activity lifecycle and set the background when neccesary.
 */
internal class BackgroundServiceFragment(
	private val backgroundService: BackgroundService
) : Fragment() {
	override fun onResume() {
		super.onResume()

		activity?.window?.decorView?.apply {
			// We need to force the system to add a new callback
			// this won't happen if we set the background to the same one
			// so we set it to null first
			if (background == backgroundService.backgroundDrawable) background = null

			background = backgroundService.backgroundDrawable
		}
	}
}
