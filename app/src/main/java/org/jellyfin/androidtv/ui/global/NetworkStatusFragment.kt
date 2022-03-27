package org.jellyfin.androidtv.ui.global

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.NetworkState
import org.jellyfin.androidtv.util.Utils
import org.koin.android.ext.android.inject

class NetworkStatusFragment : Fragment() {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val networkStatusViewModel: NetworkStatusViewModel by inject()

		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				networkStatusViewModel.getNetworkState.collect {
					postToast(it)
				}
			}
		}
	}

	private fun postToast(networkState: NetworkState) {
		if (context == null) return
		when (networkState) {
			NetworkState.CONNECTED -> Utils.showToast(context, R.string.toast_connected_network)
			NetworkState.DISCONNECTED -> Utils.showToast(context, R.string.toast_disconnected_network)
			else -> {}
		}
	}
}
