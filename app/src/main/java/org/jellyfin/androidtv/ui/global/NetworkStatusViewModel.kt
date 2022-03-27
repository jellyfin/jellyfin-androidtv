package org.jellyfin.androidtv.ui.global

import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.data.repository.NetworkState
import org.jellyfin.androidtv.data.repository.NetworkStatusRepository

class NetworkStatusViewModel (
	private val repo: NetworkStatusRepository
) : ViewModel() {
	val getNetworkState = repo.state
	fun getCurrentNetworkState() : NetworkState = repo.state.value
}
