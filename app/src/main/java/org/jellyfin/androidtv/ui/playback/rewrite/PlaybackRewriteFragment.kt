package org.jellyfin.androidtv.ui.playback.rewrite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.Queue
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.ui.PlayerSurfaceView
import org.jellyfin.playback.jellyfin.queue.createBaseItemQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

/**
 * Temporary fragment used for testing the playback rewrite. This will eventually be replaced with a
 * proper player user interface.
 */
class PlaybackRewriteFragment : Fragment() {
	companion object {
		const val EXTRA_ITEM_ID: String = "item_id"
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val playbackManager by inject<PlaybackManager>()
	private val api by inject<ApiClient>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Try find the item id
		val itemId = findItemId()

		if (itemId == null) {
			Toast.makeText(
				requireContext(),
				"Could not find item to play (itemId=null)",
				Toast.LENGTH_LONG
			).show()
			return
		}

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
				// Get fresh info from API in SDK format
				val item by api.userLibraryApi.getItem(itemId = itemId)

				// Log info
				Toast.makeText(
					requireContext(),
					"Found item of type ${item.type} - ${item.name} (${item.id}",
					Toast.LENGTH_LONG
				).show()
				Timber.i(item.toString())

				// TODO: Dirty hack to create a single item queue
				val queueEntry = createBaseItemQueueEntry(api, item)
				playbackManager.state.queue.replaceQueue(object : Queue {
					override val size: Int = 1

					override suspend fun getItem(index: Int): QueueEntry? {
						if (index == 0) return queueEntry
						return null
					}
				})
			}
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = PlayerSurfaceView(requireContext())
		view.playbackManager = playbackManager
		return view
	}

	private fun findItemId(): UUID? {
		val extra = requireArguments().getString(EXTRA_ITEM_ID)?.toUUIDOrNull()

		var first: BaseItemDto? = null
		var best: BaseItemDto? = null

		for (item in videoQueueManager.getCurrentVideoQueue()) {
			if (first == null) first = item
			if (item.type !== BaseItemKind.TRAILER) best = item

			if (best != null) break
		}

		return extra ?: best?.id ?: first?.id
	}
}
