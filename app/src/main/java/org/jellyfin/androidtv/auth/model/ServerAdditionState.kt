package org.jellyfin.androidtv.auth.model

import org.jellyfin.apiclient.model.system.PublicSystemInfo

sealed class ServerAdditionState
object ConnectingState : ServerAdditionState()
data class UnableToConnectState(val error: Exception) : ServerAdditionState()
data class ConnectedState(val publicInfo: PublicSystemInfo) : ServerAdditionState()
