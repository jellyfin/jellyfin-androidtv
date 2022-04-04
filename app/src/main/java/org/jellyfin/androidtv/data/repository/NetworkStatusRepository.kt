package org.jellyfin.androidtv.data.repository

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import org.jellyfin.androidtv.constant.NetworkState
import timber.log.Timber

interface NetworkStatusRepository {
	val state: StateFlow<NetworkState>
}

/**
 * Registers itself to monitor the application network status
 * and provides a StateFlow to monitor the network status
 */
class NetworkStatusRepositoryImpl(
	appScope: CoroutineScope,
	connectivityManager: ConnectivityManager,
	private val networkRequestBuilderDi: NetworkRequest.Builder? = null
) : NetworkStatusRepository {

	private val _state = MutableStateFlow(NetworkState.UNKNOWN)
	override val state = _state.asStateFlow()

	init {
		// Since network is integral to this app, we will run the callback with the app lifecycle
		connectivityManager.registerNetworkCallback(
			buildMonitoredTransportTypes(),
			buildNetworkCallback()
		)
		_state.subscriptionCount.launchIn(appScope)
	}

	fun notifyNetworkAvailable() {
		Timber.i("NetworkStatus: Connected")
		_state.update { NetworkState.CONNECTED }
	}

	fun notifyNetworkDisconnect() {
		Timber.i("NetworkStatus: Disconnected")
		_state.update { NetworkState.DISCONNECTED }
	}

	private fun buildMonitoredTransportTypes(): NetworkRequest {
		val realBuilder = networkRequestBuilderDi ?: NetworkRequest.Builder()
		return realBuilder
			.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
			.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
			.build()
	}

	private fun buildNetworkCallback(): ConnectivityManager.NetworkCallback {
		return object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				notifyNetworkAvailable()
			}

			override fun onLost(network: Network) {
				notifyNetworkDisconnect()
			}
		}
	}
}

