package org.jellyfin.androidtv.data.repository

sealed class LoginState
object AuthenticatingState : LoginState()
object RequireSignInState : LoginState()
object ServerUnavailableState : LoginState()
object AuthenticatedState : LoginState()
