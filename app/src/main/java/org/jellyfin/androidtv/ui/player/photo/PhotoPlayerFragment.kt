package org.jellyfin.androidtv.ui.player.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

class PhotoPlayerFragment : Fragment() {
	companion object {
		const val ARGUMENT_ITEM_ID = "item_id"
		const val ARGUMENT_ALBUM_SORT_BY = "album_sort_by"
		const val ARGUMENT_ALBUM_SORT_ORDER = "album_sort_order"
		const val ARGUMENT_AUTO_PLAY = "auto_play"
	}

	private val viewModel by viewModel<PhotoPlayerViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Load requested item in viewmodel
		lifecycleScope.launch {
			val itemId = requireNotNull(arguments?.getString(ARGUMENT_ITEM_ID)?.toUUIDOrNull())
			val albumSortBy = arguments?.getString(ARGUMENT_ALBUM_SORT_BY)?.let {
				ItemSortBy.Companion.fromNameOrNull(it)
			} ?: ItemSortBy.SORT_NAME
			val albumSortOrder = arguments?.getString(ARGUMENT_ALBUM_SORT_ORDER)?.let {
				SortOrder.Companion.fromNameOrNull(it)
			} ?: SortOrder.ASCENDING
			viewModel.loadItem(itemId, setOf(albumSortBy), albumSortOrder)

			val autoPlay = arguments?.getBoolean(ARGUMENT_AUTO_PLAY) == true
			if (autoPlay) viewModel.startPresentation()
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			PhotoPlayerScreen()
		}
	}
}
