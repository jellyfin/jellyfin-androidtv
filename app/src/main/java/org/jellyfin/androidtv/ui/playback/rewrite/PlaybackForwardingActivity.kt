package org.jellyfin.androidtv.ui.playback.rewrite

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import org.jellyfin.androidtv.di.userApiClient
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.sdk.api.operations.UserLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

class PlaybackForwardingActivity : FragmentActivity() {
	private val mediaManager by inject<MediaManager>()
	private val userLibraryApi by lazy {
		UserLibraryApi(get(userApiClient))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Try find the item id
		val itemId = findItem()?.id?.toUUIDOrNull()

		if (itemId == null) {
			Toast.makeText(
				this,
				"Could not find item to play (itemId=null)",
				Toast.LENGTH_LONG
			).show()
			finishAfterTransition()
			return
		}

		lifecycleScope.launchWhenCreated {
			// Get fresh info from API in SDK format
			val item by userLibraryApi.getItem(itemId = itemId)

			// Log info
			Toast.makeText(
				this@PlaybackForwardingActivity,
				"Found item of type ${item.type} - ${item.name} (${item.id}",
				Toast.LENGTH_LONG
			).show()
			Timber.i(item.toString())

			// TODO: Create queue, send to new playback manager, start new player UI
			finishAfterTransition()
		}
	}

	private fun findItem(): BaseItemDto? {
		var first: BaseItemDto? = null
		var best: BaseItemDto? = null

		for (item in mediaManager.currentVideoQueue) {
			if (first == null) first = item
			if (best == null && item.baseItemType !== BaseItemType.Trailer) best = item

			if (first != null && best != null) break
		}

		return best ?: first
	}
}
