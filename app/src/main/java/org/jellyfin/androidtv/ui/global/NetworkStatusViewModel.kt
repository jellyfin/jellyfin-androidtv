package org.jellyfin.androidtv.ui.global

import androidx.lifecycle.ViewModel
import org.jellyfin.androidtv.data.repository.NetworkStatusRepository

class NetworkStatusViewModel (
	repo: NetworkStatusRepository
) : ViewModel() {
	val getNetworkState = repo.state
}
