package org.jellyfin.androidtv.data.repository

import android.content.Context
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
import timber.log.Timber


enum class NetworkState {
	CONNECTED,
	DISCONNECTED,
	UNKNOWN
}

interface NetworkStatusRepository {
	val state: StateFlow<NetworkState>
}

/**
 * Registers itself to monitor the application network status
 * and provides a StateFlow to monitor the network status
 */
class NetworkStatusRepositoryImpl(
	appContext: Context,
	appScope: CoroutineScope,
	// DI injection to make repo instantiation much easier in tests:
	connectivityManagerDi: ConnectivityManager? = null,
	private val networkRequestBuilderDi: NetworkRequest.Builder? = null
) : NetworkStatusRepository {

	private val _state = MutableStateFlow(NetworkState.UNKNOWN)
	override val state = _state.asStateFlow()

	// Some manual DI, since using Koin here is overkill
	private val connectivityManager = connectivityManagerDi
		?: appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

