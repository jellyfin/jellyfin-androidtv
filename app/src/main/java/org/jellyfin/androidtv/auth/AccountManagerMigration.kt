package org.jellyfin.androidtv.auth

import android.accounts.AccountManager
import android.content.Context
import androidx.core.content.getSystemService
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber
import java.util.UUID

class AccountManagerMigration(
	context: Context,
) {
	private val accountManager = requireNotNull(context.getSystemService<AccountManager>())

	fun migrate(
		servers: Map<UUID, AuthenticationStoreServer>,
	) = servers.mapValues { (serverId, server) ->
		Timber.i("Migrating server $serverId (${server.name})")
		server.copy(
			users = server.users.mapValues { (userId, user) ->
				val accessToken = getAccessToken(serverId, userId)
				Timber.i("Migrating user $userId (${user.name}): ${if (accessToken != null) "success" else "no token"}")
				user.copy(accessToken = accessToken)
			}
		)
	}

	@Suppress("MissingPermission")
	private fun getAccessToken(serverId: UUID, userId: UUID): String? = runCatching {
		accountManager.getAccountsByType(ACCOUNT_TYPE)
			.firstOrNull {
				val validServerId = accountManager.getUserData(it, ACCOUNT_DATA_SERVER)?.toUUIDOrNull() == serverId
				val validUserId = accountManager.getUserData(it, ACCOUNT_DATA_ID)?.toUUIDOrNull() == userId

				validServerId && validUserId
			}
			?.let { account -> accountManager.peekAuthToken(account, ACCOUNT_ACCESS_TOKEN_TYPE) }
	}.getOrNull()

	companion object {
		const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID
		const val ACCOUNT_DATA_ID = "$ACCOUNT_TYPE.id"
		const val ACCOUNT_DATA_SERVER = "$ACCOUNT_TYPE.server"
		const val ACCOUNT_ACCESS_TOKEN_TYPE = "$ACCOUNT_TYPE.access_token"
	}
}
