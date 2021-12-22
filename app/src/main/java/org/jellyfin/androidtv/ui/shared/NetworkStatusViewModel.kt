package org.jellyfin.androidtv.ui.shared

import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.data.repository.NetworkState
import org.jellyfin.androidtv.data.repository.NetworkStatusRepository

class NetworkStatusViewModel (
	private val repo: NetworkStatusRepository
) : ViewModel() {
	val networkState = repo.state
	fun getNetworkState() : NetworkState = repo.state.value
}
