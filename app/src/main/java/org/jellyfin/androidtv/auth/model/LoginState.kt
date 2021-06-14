package org.jellyfin.androidtv.auth.model

sealed class LoginState
object AuthenticatingState : LoginState()
object RequireSignInState : LoginState()
object ServerUnavailableState : LoginState()
data class ServerVersionNotSupported(val server: Server) : LoginState()
object AuthenticatedState : LoginState()
