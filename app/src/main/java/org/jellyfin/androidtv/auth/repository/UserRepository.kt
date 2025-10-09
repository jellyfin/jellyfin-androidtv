package org.jellyfin.androidtv.auth.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.api.UserDto

/**
 * Repository to get the current authenticated user.
 */
interface UserRepository {
	val currentUser: StateFlow<UserDto?>

	fun setCurrentUser(user: UserDto?)
}

class UserRepositoryImpl : UserRepository {
	override val currentUser = MutableStateFlow<UserDto?>(null)

	override fun setCurrentUser(user: UserDto?) {
		currentUser.value = user
	}
}
