package org.jellyfin.androidtv.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.api.UserDto

interface UserRepository {
	val currentUser: StateFlow<UserDto?>

	fun updateCurrentUser(user: UserDto?)
}

class UserRepositoryImpl : UserRepository {
	override val currentUser = MutableStateFlow<UserDto?>(null)

	override fun updateCurrentUser(user: UserDto?) {
		if (currentUser.value !== user) currentUser.value = user
	}
}
