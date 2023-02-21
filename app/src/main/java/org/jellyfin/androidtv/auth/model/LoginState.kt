package org.jellyfin.androidtv.auth.model

import org.jellyfin.sdk.api.client.exception.ApiClientException

sealed class LoginState
object AuthenticatingState : LoginState()
object RequireSignInState : LoginState()
object ServerUnavailableState : LoginState()
data class ServerVersionNotSupported(val server: Server) : LoginState()
data class ApiClientErrorLoginState(val error: ApiClientException) : LoginState()
object AuthenticatedState : LoginState()
