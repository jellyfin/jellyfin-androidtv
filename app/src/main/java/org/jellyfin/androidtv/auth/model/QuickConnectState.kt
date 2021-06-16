package org.jellyfin.androidtv.auth.model

sealed class QuickConnectState

/**
 * State unknown untill first poll completed.
 */
object UnknownQuickConnectState : QuickConnectState()

/**
 * Server does not have QuickConnect enabled.
 */
object UnavailableQuickConnectState : QuickConnectState()

/**
 * Connection is pending.
 */
data class PendingQuickConnectState(val code: String) : QuickConnectState()

/**
 * User connected.
 */
object ConnectedQuickConnectState : QuickConnectState()
