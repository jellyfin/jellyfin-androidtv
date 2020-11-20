package org.jellyfin.androidtv.auth.model

data class AccountManagerAccount(
	val id: String,
	val server: String,
	val name: String,
	val accessToken: String?,
)
