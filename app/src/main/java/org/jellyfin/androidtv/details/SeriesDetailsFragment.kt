package org.jellyfin.androidtv.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentContainer
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.databinding.FragmentSeriesDetailsBinding
import org.jellyfin.androidtv.model.itemtypes.Series
import org.jellyfin.androidtv.util.apiClient
import org.jellyfin.androidtv.util.apiclient.getEpisodesOfSeason
import org.jellyfin.androidtv.util.apiclient.getSeasonsOfSeries

/**
 * A simple [Fragment] subclass.
 */
class SeriesDetailsFragment(val series: Series) : Fragment() {
	private lateinit var binding: FragmentSeriesDetailsBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		// Inflate the layout for this fragment
		binding = FragmentSeriesDetailsBinding.inflate(inflater, container, false)

		binding.title.text = series.title

		val episodeAdapter = EpisodeAdapter { episode, _, focus ->
			if (focus) {
				val transaction = parentFragmentManager.beginTransaction()
				val fragment = SeriesDetailsSidebarFragment(episode)
				transaction.replace(binding.sidebar.id, fragment)
				transaction.commit()
			}
		}

		binding.recycler.apply {
			adapter = episodeAdapter
			layoutManager = LinearLayoutManager(context)
		}

		binding.seasonSwitch.setOnFocusChangeListener { _, focus ->
			if (focus) {
				val transaction = parentFragmentManager.beginTransaction()
				val fragment = SeriesDetailsSidebarFragment(series)
				transaction.replace(binding.sidebar.id, fragment)
				transaction.commit()
			}
		}

		binding.seasonSwitch.requestFocus()

		lifecycleScope.launch {
			val seasons = apiClient.getSeasonsOfSeries(series.id)

			if (seasons == null || seasons.isEmpty()) {
				// Fixme: Localize
				Toast.makeText(context, "Failed to load seasons", Toast.LENGTH_LONG).show()
				return@launch
			}

			val season = seasons[0]

			binding.season.text = season.title

			val episodes = apiClient.getEpisodesOfSeason(season.id)

			if (episodes == null) {
				// Fixme: Localize
				Toast.makeText(context, "Failed to load episodes", Toast.LENGTH_LONG).show()
				return@launch
			}

			episodeAdapter.setItems(episodes)
		}

		binding.recycler.setOnFocusChangeListener { _, focus ->
			if (focus) {
				binding.title.visibility = View.VISIBLE
				binding.headButtons.visibility = View.INVISIBLE
			} else {
				binding.title.visibility = View.INVISIBLE
				binding.headButtons.visibility = View.VISIBLE
			}
		}

		return binding.root
	}

	companion object {
		private const val TAG = "SeriesDetailsFragment"
	}
}
