package org.jellyfin.androidtv.auth.model

import org.jellyfin.apiclient.model.system.PublicSystemInfo

sealed class ServerAdditionState
data class ConnectingState(val address: String) : ServerAdditionState()
data class UnableToConnectState(val error: Exception) : ServerAdditionState()
data class ConnectedState(val publicInfo: PublicSystemInfo) : ServerAdditionState()
