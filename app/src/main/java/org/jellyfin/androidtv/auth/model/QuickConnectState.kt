package org.jellyfin.androidtv.auth.model

sealed class QuickConnectState

/**
 * State unknown until first poll completed.
 */
data object UnknownQuickConnectState : QuickConnectState()

/**
 * Server does not have QuickConnect enabled.
 */
data object UnavailableQuickConnectState : QuickConnectState()

/**
 * Connection is pending.
 */
data class PendingQuickConnectState(val code: String) : QuickConnectState()

/**
 * User connected.
 */
data object ConnectedQuickConnectState : QuickConnectState()
