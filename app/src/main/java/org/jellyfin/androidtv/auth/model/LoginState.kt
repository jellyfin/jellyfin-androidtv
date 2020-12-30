package org.jellyfin.androidtv.auth.model

sealed class LoginState
object AuthenticatingState : LoginState()
object RequireSignInState : LoginState()
object ServerUnavailableState : LoginState()
object AuthenticatedState : LoginState()
