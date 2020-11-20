package org.jellyfin.androidtv.auth

import android.accounts.AccountManager

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

	fun getServers() = this.authenticationStore.getServers()
}
