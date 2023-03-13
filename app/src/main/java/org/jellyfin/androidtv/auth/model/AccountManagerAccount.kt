package org.jellyfin.androidtv.auth.model

import java.util.UUID

data class AccountManagerAccount(
	val id: UUID,
	val address: String,
	val server: UUID,
	val name: String,
	val accessToken: String? = null,
)
