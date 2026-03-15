package org.jellyfin.androidtv.ui.livetv

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.databinding.LiveTvGuideBinding
import org.jellyfin.androidtv.ui.GuideChannelHeader
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

fun pageGuideChannels(
	activity: Activity,
	programRows: ViewGroup,
	channels: ViewGroup,
	visibleRows: Int,
	forward: Boolean,
): Boolean {
	val focused = activity.currentFocus ?: return false

	// Find which row currently has focus (check both program rows and channel headers)
	var currentRowNdx = -1
	var focusOnChannelHeader = false

	for (i in 0 until programRows.childCount) {
		val row = programRows.getChildAt(i)
		if (row == focused || (row is ViewGroup && row.indexOfChild(focused) >= 0)) {
			currentRowNdx = i
			break
		}
	}

	if (currentRowNdx < 0) {
		// Check if focus is on a channel header
		for (i in 0 until channels.childCount) {
			if (focused == channels.getChildAt(i)) {
				currentRowNdx = i
				focusOnChannelHeader = true
				break
			}
		}
	}

	if (currentRowNdx < 0) return false

	// Calculate target row, clamped to valid range
	val targetRowNdx = if (forward) {
		minOf(currentRowNdx + visibleRows, programRows.childCount - 1)
	} else {
		maxOf(currentRowNdx - visibleRows, 0)
	}

	if (focusOnChannelHeader) {
		channels.getChildAt(targetRowNdx)?.requestFocus()
	} else {
		val targetRow = programRows.getChildAt(targetRowNdx)
		if (targetRow is ViewGroup) {
			// Find the child at the same horizontal position to preserve time scroll position
			val focusedLeft = focused.left
			val best = (0 until targetRow.childCount)
				.map { targetRow.getChildAt(it) }
				.firstOrNull { it.left <= focusedLeft && it.right > focusedLeft }
				?: targetRow
			best.requestFocus()
		} else {
			targetRow?.requestFocus()
		}
	}

	return true
}

fun createNoProgramDataBaseItem(
	context: Context,
	channelId: UUID?,
	startDate: LocalDateTime?,
	endDate: LocalDateTime?
) = BaseItemDto(
	id = UUID.randomUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	name = context.getString(R.string.no_program_data),
	channelId = channelId,
	startDate = startDate,
	endDate = endDate,
)

fun LiveTvGuideFragment.toggleFavorite() {
	val header = mSelectedProgramView as? GuideChannelHeader
	val channel = header?.channel ?: return

	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		runCatching {
			val userData = itemMutationRepository.setFavorite(
				item = header.channel.id,
				favorite = !(channel.userData?.isFavorite ?: false)
			)

			header.channel = header.channel.copy(userData = userData)
			header.findViewById<View>(R.id.favImage).isVisible = userData.isFavorite
			dataRefreshService.lastFavoriteUpdate = Instant.now()
		}
	}
}

fun LiveTvGuideFragment.refreshSelectedProgram() {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			val item = withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(mSelectedProgram.id).content
			}
			mSelectedProgram = item
		}.onFailure { error ->
			Timber.e(error, "Unable to get program details")
		}

		detailUpdateInternal();
	}
}

fun LiveTvGuideFragment.addSettingsOptions(binding: LiveTvGuideBinding): MutableStateFlow<Boolean> {
	val visible = MutableStateFlow(false)

	binding.settingsOptions.setContent {
		val isVisible by visible.collectAsState(false)

		JellyfinTheme {
			ProvideRouter(
				routes,
				Routes.LIVETV_GUIDE_OPTIONS
			) {
				SettingsDialog(
					visible = isVisible,
					onDismissRequest = {
						visible.value = false
						TvManager.forceReload()
						doLoad()
					}
				) {
					SettingsRouterContent()
				}
			}
		}
	}

	return visible
}

fun LiveTvGuideFragment.addSettingsFilters(binding: LiveTvGuideBinding): MutableStateFlow<Boolean> {
	val visible = MutableStateFlow(false)

	binding.settingsFilters.setContent {
		val isVisible by visible.collectAsState(false)

		JellyfinTheme {
			ProvideRouter(
				routes,
				Routes.LIVETV_GUIDE_FILTERS
			) {
				SettingsDialog(
					visible = isVisible,
					onDismissRequest = {
						visible.value = false
						TvManager.forceReload()
						doLoad()
					}
				) {
					SettingsRouterContent()
				}
			}
		}
	}

	return visible
}
