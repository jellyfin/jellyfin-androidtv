package org.jellyfin.androidtv.data.model

class ServerList(
	var currentServerUsers: Map<Server, List<User>> = emptyMap(),
	var currentServerUsersState: LoadingState = LoadingState.PENDING,
	var discoveredServersUsers: Map<Server, List<User>> = emptyMap(),
	var discoveredServersUsersState: LoadingState = LoadingState.PENDING,
	var savedServersUsers: Map<Server, List<User>> = emptyMap(),
	var savedServersUsersState: LoadingState = LoadingState.PENDING
) {
	val state: LoadingState
		get() {
			return if (currentServerUsersState.status == LoadingState.error().status) {
				currentServerUsersState
			} else if (discoveredServersUsersState.status == LoadingState.error().status) {
				discoveredServersUsersState
			} else if (savedServersUsersState.status == LoadingState.error().status) {
				savedServersUsersState
			} else if (currentServerUsersState == LoadingState.LOADING ||
					discoveredServersUsersState == LoadingState.LOADING ||
					savedServersUsersState == LoadingState.LOADING) {
				LoadingState.LOADING
			} else if (currentServerUsersState == LoadingState.SUCCESS ||
					discoveredServersUsersState == LoadingState.SUCCESS ||
					savedServersUsersState == LoadingState.SUCCESS) {
				LoadingState.SUCCESS
			} else {
				LoadingState.PENDING
			}
		}

	val allServersUsers: Map<Server, List<User>>
		get() = currentServerUsers + savedServersUsers + discoveredServersUsers
}
