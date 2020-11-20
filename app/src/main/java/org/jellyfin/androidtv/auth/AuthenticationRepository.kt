package org.jellyfin.androidtv.auth

import android.accounts.AccountManager
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import java.util.*

class AuthenticationRepository(
	private val accountManager: AccountManager,
	private val authenticationStore: AuthenticationStore
) {
	/**
	 * Remove accounts from authentication store that are not in the account manager.
	 */
	fun sync() {
		val savedAccountIds = accountManager.getJellyfinAccounts().map { it.id }

		authenticationStore.getServers().forEach { (serverId, server) ->
			server.users.forEach { (userId, _) ->
				if (!savedAccountIds.contains(userId))
					authenticationStore.removeUser(serverId, userId)
			}
		}
	}

	fun getServers() = this.authenticationStore.getServers().map { (id, info) ->
		Server(id.toString(), info.name, info.url, Date(info.lastUsed))
	}

	fun getUsers(): Map<Server, List<User>> = this.authenticationStore.getServers().map { (serverId, serverInfo) ->
		Server(serverId.toString(), serverInfo.name, serverInfo.url, Date(serverInfo.lastUsed)) to serverInfo.users.map { (userId, userInfo) ->
			val authInfo = this.accountManager.getJellyfinAccount(userId)

			User(userId.toString(), userInfo.name, authInfo?.accessToken ?: "", serverId.toString(), userInfo.profile_picture)
		}
	}.toMap()

	suspend fun login(server: Server, name: String, password: String) {
		val userId = UUID.randomUUID().toString()
		this.authenticationStore.putUser(server.id, userId, AuthenticationStoreUser(name, "", Date().time, 0))
		this.accountManager.putJellyfinAccount(AccountManagerAccount(userId, server.id, name, null))
	}
}
