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
 * Displays episodes for a specific season using Jetpack Compose
 */
class ComposeSeasonFragment : Fragment() {

	companion object {
		private const val ARG_SEASON_ID = "season_id"

		fun newInstance(seasonId: UUID): ComposeSeasonFragment {
			return ComposeSeasonFragment().apply {
				arguments = Bundle().apply {
					putString(ARG_SEASON_ID, seasonId.toString())
				}
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		val seasonId = UUID.fromString(
			arguments?.getString(ARG_SEASON_ID)
				?: throw IllegalArgumentException("Season ID is required"),
		)

		return ComposeView(requireContext()).apply {
			setContent {
				JellyfinTvTheme {
					ComposeSeasonScreen(seasonId = seasonId)
				}
			}
		}
	}
}
