package org.jellyfin.androidtv.auth.model

import java.util.*

data class AccountManagerAccount(
	val id: UUID,
	val server: UUID,
	val name: String,
	val accessToken: String? = null,
)
