package org.jellyfin.androidtv.data.model

import org.junit.Assert.*
import org.junit.Test

class ServerListTest {
	@Test
	fun defaultState() {
		val serverList = ServerList()
		assertTrue(serverList.currentServerUsers.isEmpty())
		assertEquals(LoadingState.PENDING.status, serverList.currentServerUsersState.status)
		assertTrue(serverList.discoveredServersUsers.isEmpty())
		assertEquals(LoadingState.PENDING.status, serverList.discoveredServersUsersState.status)
		assertTrue(serverList.savedServersUsers.isEmpty())
		assertEquals(LoadingState.PENDING.status, serverList.savedServersUsersState.status)

		assertEquals(LoadingState.PENDING.status, serverList.state.status)
		assertTrue(serverList.allServersUsers.isEmpty())
	}

	@Test
	fun errorState() {
		val errorState = LoadingState.error("ERROR!")
		var serverList = ServerList(
			currentServerUsersState = errorState,
			discoveredServersUsersState = LoadingState.SUCCESS,
			savedServersUsersState = LoadingState.LOADING
		)
		assertEquals(errorState, serverList.state)
		serverList = ServerList(
			currentServerUsersState = LoadingState.SUCCESS,
			discoveredServersUsersState = errorState,
			savedServersUsersState = LoadingState.LOADING
		)
		assertEquals(errorState, serverList.state)
		serverList = ServerList(
			currentServerUsersState = LoadingState.LOADING,
			discoveredServersUsersState = LoadingState.SUCCESS,
			savedServersUsersState = errorState
		)
		assertEquals(errorState, serverList.state)
	}

	@Test
	fun loadingState() {
		var serverList = ServerList(
			currentServerUsersState = LoadingState.LOADING,
			discoveredServersUsersState = LoadingState.SUCCESS
		)
		assertEquals(LoadingState.LOADING, serverList.state)
		serverList = ServerList(
			currentServerUsersState = LoadingState.SUCCESS,
			discoveredServersUsersState = LoadingState.LOADING
		)
		assertEquals(LoadingState.LOADING, serverList.state)
		serverList = ServerList(
			currentServerUsersState = LoadingState.SUCCESS,
			savedServersUsersState = LoadingState.LOADING
		)
		assertEquals(LoadingState.LOADING, serverList.state)
	}

	@Test
	fun successState() {
		var serverList = ServerList(currentServerUsersState = LoadingState.SUCCESS)
		assertEquals(LoadingState.SUCCESS, serverList.state)
		serverList = ServerList(discoveredServersUsersState = LoadingState.SUCCESS)
		assertEquals(LoadingState.SUCCESS, serverList.state)
		serverList = ServerList(savedServersUsersState = LoadingState.SUCCESS)
		assertEquals(LoadingState.SUCCESS, serverList.state)
	}
}
