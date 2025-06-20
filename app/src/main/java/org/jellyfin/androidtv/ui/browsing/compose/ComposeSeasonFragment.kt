package org.jellyfin.androidtv.ui.browsing.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme
import java.util.UUID

/**
 * Fragment wrapper for ComposeSeasonScreen
 * Displays seasons for a specific TV show using Jetpack Compose
 */
class ComposeSeasonFragment : Fragment() {

	companion object {
		private const val ARG_SERIES_ID = "series_id"

		fun newInstance(seriesId: UUID): ComposeSeasonFragment {
			return ComposeSeasonFragment().apply {
				arguments = Bundle().apply {
					putString(ARG_SERIES_ID, seriesId.toString())
				}
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val seriesId = UUID.fromString(
			arguments?.getString(ARG_SERIES_ID)
				?: throw IllegalArgumentException("Series ID is required"),
		)

		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTvTheme {
					ComposeSeasonScreen(seriesId = seriesId)
				}
			}
		}
	}
}
