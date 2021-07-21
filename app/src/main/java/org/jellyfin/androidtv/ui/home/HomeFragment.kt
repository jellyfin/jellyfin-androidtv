package org.jellyfin.androidtv.ui.home

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.browsing.IRowLoader
import org.jellyfin.androidtv.ui.browsing.StdRowsFragment
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.util.apiclient.callApi
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.entities.DisplayPreferences
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery
import org.jellyfin.apiclient.model.querying.ItemsResult
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*

class HomeFragment : StdRowsFragment(), AudioEventListener {
	private val apiClient by inject<ApiClient>()
	private val mediaManager by inject<MediaManager>()
	private val helper by lazy { HomeFragmentHelper(requireContext()) }

	// Data
	private val rows = mutableListOf<HomeFragmentRow>()
	private var views: ItemsResult? = null
	private var includeLiveTvRows: Boolean = false

	// Special rows
	private val nowPlaying by lazy { HomeFragmentNowPlayingRow(requireActivity(), mediaManager) }
	private val liveTVRow by lazy { HomeFragmentLiveTVRow(requireActivity(), get()) }

	override fun onCreate(savedInstanceState: Bundle?) {
		// Create adapter/presenter and set it to parent
		mRowsAdapter = ArrayObjectAdapter(PositionableListRowPresenter())
		mCardPresenter = CardPresenter()
		adapter = mRowsAdapter

		super.onCreate(savedInstanceState)

		// Subscribe to Audio messages
		mediaManager.addAudioEventListener(this)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Make sure to focus the cards instead of the toolbar
		ViewCompat.setFocusedByDefault(view, true)
	}

	override fun onResume() {
		super.onResume()

		// Update audio queue
		Timber.i("Updating audio queue in HomeFragment (onResume)")
		nowPlaying.update(mRowsAdapter)

		if (helper.hasResumeRow(mRowsAdapter)) refreshRows()
	}

	override fun onQueueStatusChanged(hasQueue: Boolean) {
		Timber.i("Updating audio queue in HomeFragment (onQueueStatusChanged)")
		nowPlaying.update(mRowsAdapter)
	}

	override fun onDestroy() {
		super.onDestroy()

		mediaManager.removeAudioEventListener(this)
	}

	override fun setupEventListeners() {
		super.setupEventListeners()

		mClickedListener.registerListener(liveTVRow::onItemClicked)
	}

	fun addSection(type: HomeSectionType) {
		when (type) {
			HomeSectionType.LATEST_MEDIA -> rows.add(helper.loadRecentlyAdded(views!!))
			HomeSectionType.LIBRARY_TILES_SMALL -> rows.add(helper.loadLibraryTiles())
			HomeSectionType.LIBRARY_BUTTONS -> rows.add(helper.loadLibraryTiles())
			HomeSectionType.RESUME -> rows.add(helper.loadResumeVideo())
			HomeSectionType.RESUME_AUDIO -> rows.add(helper.loadResumeAudio())
			HomeSectionType.ACTIVE_RECORDINGS -> rows.add(helper.loadLatestLiveTvRecordings())
			HomeSectionType.NEXT_UP -> rows.add(helper.loadNextUp())
			HomeSectionType.LIVE_TV -> if (includeLiveTvRows) {
				rows.add(liveTVRow)
				rows.add(helper.loadOnNow())
			}
			HomeSectionType.NONE -> Unit
		}
	}

	override fun setupQueries(rowLoader: IRowLoader) {
		lifecycleScope.launch(Dispatchers.IO) {
			val currentUser = TvApp.getApplication()!!.currentUser!!
			// Update the views before creating rows
			views = callApi<ItemsResult> { apiClient.GetUserViews(currentUser.id, it) }

			// Start out with default sections
			val homesections = DEFAULT_SECTIONS.toMutableMap()

			try {
				// Get display preferences
				val prefs = callApi<DisplayPreferences> {
					TvApp.getApplication()?.getDisplayPrefsAsync("usersettings", "emby", it)
				}.customPrefs

				// Add sections from preferences
				prefs.forEach { (key, value) ->
					// Not a homesection key
					if (!key.startsWith("homesection")) return@forEach

					// Parse data
					val index = key.removePrefix("homesection").toIntOrNull() ?: return@forEach
					val type = HomeSectionType.getById(value) ?: return@forEach

					homesections[index] = type
				}
			} catch (exception: Exception) {
				Timber.e(exception, "Unable to retrieve home sections")
			}

			// Check for live TV support
			if (homesections.containsValue(HomeSectionType.LIVE_TV) && currentUser.policy.enableLiveTvAccess) {
				// This is kind of ugly, but it mirrors how web handles the live TV rows on the home screen
				// If we can retrieve one live TV recommendation, then we should display the rows
				callApi<ItemsResult> {
					apiClient.GetRecommendedLiveTvProgramsAsync(
						RecommendedProgramQuery().apply {
							userId = currentUser.id
							enableTotalRecordCount = false
							imageTypeLimit = 1
							isAiring = true
							limit = 1
						},
						it
					)
				}.let { includeLiveTvRows = !it.items.isNullOrEmpty() }
			}

			// Make sure the rows are empty
			rows.clear()

			// Check for coroutine cancellation
			if (!isActive) return@launch

			// Actually add the sections
			homesections.forEach { section -> addSection(section.value) }

			// Add sections to layout
			withContext(Dispatchers.Main) {
				// Add rows in order
				nowPlaying.addToRowsAdapter(mCardPresenter, mRowsAdapter)
				for (row in rows) row.addToRowsAdapter(mCardPresenter, mRowsAdapter)

				// Manually set focus if focusedByDefault is not available
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) view?.requestFocus()
			}
		}
	}

	override fun loadRows(rows: List<BrowseRowDef>) {
		// Override to make sure it is ignored because we manage our own rows
	}

	companion object {
		private val DEFAULT_SECTIONS = mapOf(
			0 to HomeSectionType.LIBRARY_TILES_SMALL,
			1 to HomeSectionType.RESUME,
			2 to HomeSectionType.RESUME_AUDIO,
			3 to HomeSectionType.LIVE_TV,
			4 to HomeSectionType.NEXT_UP,
			5 to HomeSectionType.LATEST_MEDIA,
			6 to HomeSectionType.NONE
		)
	}
}
