package org.jellyfin.androidtv.ui.picture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

class PictureViewerActivity : FragmentActivity(R.layout.fragment_content_view) {
	companion object {
		const val EXTRA_ITEM_ID = "item_id"
		const val EXTRA_ALBUM_SORT_BY = "album_sort_by"
		const val EXTRA_ALBUM_SORT_ORDER = "album_sort_order"
		const val EXTRA_AUTO_PLAY = "auto_play"

		fun createIntent(context: Context, item: BaseItemDto, autoPlay: Boolean, albumSortBy: String, albumSortOrder: SortOrder): Intent {
			require(item.type == BaseItemKind.PHOTO) { "Expected item of type PHOTO but got ${item.type}" }

			return Intent(context, PictureViewerActivity::class.java).apply {
				putExtra(EXTRA_ITEM_ID, item.id.toString())
				putExtra(EXTRA_ALBUM_SORT_BY, albumSortBy)
				putExtra(EXTRA_ALBUM_SORT_ORDER, Json.Default.encodeToString(albumSortOrder))
				putExtra(EXTRA_AUTO_PLAY, autoPlay)
			}
		}
	}

	private val pictureViewerViewModel by viewModel<PictureViewerViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Load requested item in viewmodel
		lifecycleScope.launch {
			val itemId = requireNotNull(intent.extras?.getString(EXTRA_ITEM_ID)?.toUUIDOrNull())
			val albumSortBy = requireNotNull(intent.extras?.getString(EXTRA_ALBUM_SORT_BY))
			val albumSortOrder = requireNotNull(intent.extras?.getString(EXTRA_ALBUM_SORT_ORDER)).let {
				Json.Default.decodeFromString<SortOrder>(it)
			}
			pictureViewerViewModel.loadItem(itemId, albumSortBy, albumSortOrder)

			val autoPlay = intent.extras?.getBoolean(EXTRA_AUTO_PLAY) == true
			if (autoPlay) pictureViewerViewModel.startPresentation()
		}

		// Show fragment
		supportFragmentManager.commit {
			add<PictureViewerFragment>(R.id.content_view)
		}
	}

	// Forward key events to fragments
	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean = supportFragmentManager.fragments
		.filter { it.isVisible }
		.filterIsInstance<View.OnKeyListener>()
		.any { it.onKey(currentFocus, keyCode, event) }

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyDown(keyCode, event)

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)
}
