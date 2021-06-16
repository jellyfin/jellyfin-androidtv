package org.jellyfin.androidtv.auth.model

sealed class AuthenticateMethod
data class AutomaticAuthenticateMethod(val user: User) : AuthenticateMethod()
data class CredentialAuthenticateMethod(val username: String, val password: String = "") : AuthenticateMethod()
data class QuickConnectAuthenticateMethod(val secret: String) : AuthenticateMethod()
