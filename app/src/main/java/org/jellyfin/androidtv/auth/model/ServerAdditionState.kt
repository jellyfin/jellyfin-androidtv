package org.jellyfin.androidtv.auth.model

import org.jellyfin.sdk.model.api.PublicSystemInfo

sealed class ServerAdditionState
data class ConnectingState(val address: String) : ServerAdditionState()
data class UnableToConnectState(val addressCandidates: List<String>) : ServerAdditionState()
data class ConnectedState(val publicInfo: PublicSystemInfo) : ServerAdditionState()
