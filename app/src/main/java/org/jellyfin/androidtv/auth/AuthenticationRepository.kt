package org.jellyfin.androidtv.auth

import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.util.toUUID
import java.util.*

class AuthenticationRepository(
	private val accountManagerHelper: AccountManagerHelper,
	private val authenticationStore: AuthenticationStore
) {
	/**
	 * Remove accounts from authentication store that are not in the account manager.
	 */
	fun sync() {
		val savedAccountIds = accountManagerHelper.getAccounts().map { it.id }

		authenticationStore.getServers().forEach { (serverId, server) ->
			server.users.forEach { (userId, _) ->
				if (!savedAccountIds.contains(userId))
					authenticationStore.removeUser(serverId, userId)
			}
		}
	}

	fun getServers() = authenticationStore.getServers().map { (id, info) ->
		Server(id.toString(), info.name, info.url, Date(info.lastUsed))
	}

	fun getUsers(): Map<Server, List<User>> = authenticationStore.getServers().map { (serverId, serverInfo) ->
		Server(serverId.toString(), serverInfo.name, serverInfo.url, Date(serverInfo.lastUsed)) to serverInfo.users.map { (userId, userInfo) ->
			val authInfo = accountManagerHelper.getAccount(userId)

			User(userId.toString(), userInfo.name, authInfo?.accessToken
				?: "", serverId.toString(), userInfo.profilePicture)
		}
	}.toMap()

	fun getUsersByServer(server: UUID): List<User>? = authenticationStore.getUsers(server)?.map { (userId, userInfo) ->
		val authInfo = accountManagerHelper.getAccount(userId)

		User(userId.toString(), userInfo.name, authInfo?.accessToken ?: "", authInfo?.server.toString(), userInfo.profilePicture)
	}

	suspend fun login(server: Server, name: String, password: String) {
		val userId = UUID.randomUUID().toString()
		authenticationStore.putUser(server.id.toUUID(), userId.toUUID(), AuthenticationStoreUser(name, "", Date().time, 0))
		accountManagerHelper.putAccount(AccountManagerAccount(userId.toUUID(), server.id.toUUID(), name, null))
	}
}
