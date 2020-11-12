package org.jellyfin.androidtv.data.model

class ServerList(
	var currentServerUsers: Map<Server, List<User>> = emptyMap(),
	var currentServerUsersState: LoadingState = LoadingState.PENDING,
	var discoveredServersUsers: Map<Server, List<User>> = emptyMap(),
	var discoveredServersUsersState: LoadingState = LoadingState.PENDING,
	var savedServersUsers: Map<Server, List<User>> = emptyMap(),
	var savedServersUsersState: LoadingState = LoadingState.PENDING
) {
	val state
		get() = when {
			currentServerUsersState.status == LoadingState.ERROR.status -> currentServerUsersState
			discoveredServersUsersState.status == LoadingState.ERROR.status -> discoveredServersUsersState
			savedServersUsersState.status == LoadingState.ERROR.status -> savedServersUsersState
			currentServerUsersState == LoadingState.LOADING ||
					discoveredServersUsersState == LoadingState.LOADING ||
					savedServersUsersState == LoadingState.LOADING -> LoadingState.LOADING
			currentServerUsersState == LoadingState.SUCCESS ||
					discoveredServersUsersState == LoadingState.SUCCESS ||
					savedServersUsersState == LoadingState.SUCCESS -> LoadingState.SUCCESS
			else -> LoadingState.PENDING
		}

	val allServersUsers
		get() = currentServerUsers + savedServersUsers + discoveredServersUsers
}
