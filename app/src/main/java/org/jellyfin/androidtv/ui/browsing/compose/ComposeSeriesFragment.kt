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
 * Fragment wrapper for ComposeSeriesScreen
 * Displays seasons for a specific TV series using Jetpack Compose
 */
class ComposeSeriesFragment : Fragment() {

	companion object {
		private const val ARG_SERIES_ID = "series_id"

		fun newInstance(seriesId: UUID): ComposeSeriesFragment {
			return ComposeSeriesFragment().apply {
				arguments = Bundle().apply {
					putString(ARG_SERIES_ID, seriesId.toString())
				}
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val seriesId = UUID.fromString(
			arguments?.getString("seriesId") 
				?: throw IllegalArgumentException("Series ID is required")
		)

		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTvTheme {
					ComposeSeriesScreen(seriesId = seriesId)
				}
			}
		}
	}
}
