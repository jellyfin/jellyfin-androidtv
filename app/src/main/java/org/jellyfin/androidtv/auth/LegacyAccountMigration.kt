package org.jellyfin.androidtv.auth

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.util.serializer.UUIDSerializer
import org.jellyfin.androidtv.util.toUUIDOrNull
import timber.log.Timber

class LegacyAccountMigration(
	private val context: Context,
	private val authenticationStore: AuthenticationStore,
	private val accountManagerHelper: AccountManagerHelper
) {
	companion object {
		const val LEGACY_CREDENTIAL_FILE = "org.jellyfin.androidtv.login.json"
	}

	suspend fun migrate() {
		val path = context.filesDir.resolve(LEGACY_CREDENTIAL_FILE)
		val json = Json {
			serializersModule = SerializersModule {
				contextual(UUIDSerializer)
			}
			ignoreUnknownKeys = true
		}

		if (path.exists()) {
			Timber.d("Starting migration of legacy credentials from $path")
			val root = json.parseToJsonElement(path.readText()).jsonObject
			val server = root["serverInfo"]?.jsonObject
			val user = root["userDto"]?.jsonObject

			val serverId = server?.get("Id")?.jsonPrimitive?.content?.toUUIDOrNull()
			if (serverId != null) {
				val serverName = server["Name"]?.jsonPrimitive?.content
				val serverAddress = server["Address"]?.jsonPrimitive?.content

				if (!authenticationStore.containsServer(serverId)) {
					Timber.i("Migrating server $serverId")
					authenticationStore.putServer(
						id = serverId,
						server = AuthenticationStoreServer(
							name = serverName ?: "",
							address = serverAddress ?: ""
						)
					)
				}

				val userId = user?.get("Id")?.jsonPrimitive?.content?.toUUIDOrNull()
				if (userId != null && !authenticationStore.containsUser(serverId, userId)) {
					Timber.i("Migrating user $userId")
					val name = user["Name"]?.jsonPrimitive?.content ?: userId.toString()
					authenticationStore.putUser(
						server = serverId,
						userId = userId,
						userInfo = AuthenticationStoreUser(
							name = name,
							profilePictureTag = user["PrimaryImageTag"]?.jsonPrimitive?.content
						)
					)
					accountManagerHelper.putAccount(AccountManagerAccount(
						id = userId,
						server = serverId,
						name = name,
						accessToken = user["AccessToken"]?.jsonPrimitive?.content?.let {
							// Convert empty string to null
							if (it.isBlank()) null
							else it
						}
					))
				}
			}
		} else {
			Timber.d("Skipping migration of legacy credentials from $path (file does not exist)")
		}
	}
}
