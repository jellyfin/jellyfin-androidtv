package org.jellyfin.androidtv.ui.home

import android.app.Activity
import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.LiveTvDefaultViewHelper
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import timber.log.Timber

/**
 * Enhanced version of HomeFragmentLiveTVRow that respects the user's default Live TV view preference
 */
class HomeFragmentLiveTVRowWithUserPreferenceCheck(
    private val activity: Activity,
    private val userRepository: UserRepository,
    private val navigationRepository: NavigationRepository,
    private val api: ApiClient,
    private val coroutineScope: CoroutineScope
) : HomeFragmentRow, OnItemViewClickedListener {
    override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
        val header = HeaderItem(rowsAdapter.size().toLong(), activity.getString(R.string.pref_live_tv_cat))
        val adapter = ArrayObjectAdapter(GridButtonPresenter())

        // Live TV Guide button
        adapter.add(GridButton(LiveTvOption.LIVE_TV_GUIDE_OPTION_ID, activity.getString(R.string.lbl_live_tv_guide)))
        // Live TV Recordings button
        adapter.add(GridButton(LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID, activity.getString(R.string.lbl_recorded_tv)))
        if (Utils.canManageRecordings(userRepository.currentUser.value)) {
            // Recording Schedule button
            adapter.add(GridButton(LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID, activity.getString(R.string.lbl_schedule)))
            // Recording Series button
            adapter.add(GridButton(LiveTvOption.LIVE_TV_SERIES_OPTION_ID, activity.getString(R.string.lbl_series)))
        }

        rowsAdapter.add(ListRow(header, adapter))
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item !is GridButton) return

        when (item.id) {
            LiveTvOption.LIVE_TV_GUIDE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvGuide)
            LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSchedule)
            LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvRecordings)
            LiveTvOption.LIVE_TV_SERIES_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSeriesRecordings)
        }
    }

    /**
     * Handle click on the Live TV card from the home screen
     * Checks user preferences and navigates directly to the preferred view if set
     */
    fun onLiveTvCardClicked() {
        coroutineScope.launch {
            try {
                // First, get the Live TV library item
                val liveTvItem = getLiveTvLibraryItem()

                // Then check for default view preference
                val defaultViewId = withContext(Dispatchers.IO) {
                    LiveTvDefaultViewHelper.getDefaultLiveTvView(api)
                }

                withContext(Dispatchers.Main) {
                    if (defaultViewId != null) {
                        // Navigate directly to the preferred view
                        when (defaultViewId) {
                            LiveTvOption.LIVE_TV_GUIDE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvGuide)
                            LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSchedule)
                            LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvRecordings)
                            LiveTvOption.LIVE_TV_SERIES_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSeriesRecordings)
                            else -> {
                                // Fallback to the standard Live TV selection screen
                                if (liveTvItem != null) {
                                    navigationRepository.navigate(Destinations.librarySmartScreen(liveTvItem))
                                } else {
                                    // If we couldn't get the Live TV item, navigate to the guide as a fallback
                                    navigationRepository.navigate(Destinations.liveTvGuide)
                                }
                            }
                        }
                    } else {
                        // No preference set, show the standard Live TV selection screen
                        if (liveTvItem != null) {
                            navigationRepository.navigate(Destinations.librarySmartScreen(liveTvItem))
                        } else {
                            // If we couldn't get the Live TV item, navigate to the guide as a fallback
                            navigationRepository.navigate(Destinations.liveTvGuide)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to the Live TV guide on error
                navigationRepository.navigate(Destinations.liveTvGuide)
            }
        }
    }

    /**
     * Gets the Live TV library item from the user's views
     */
    private suspend fun getLiveTvLibraryItem(): BaseItemDto? {
        return try {
            val userViews = api.userViewsApi.getUserViews().content?.items ?: emptyList()
            userViews.find { it.collectionType == CollectionType.LIVETV }
        } catch (e: Exception) {
            null
        }
    }
}
