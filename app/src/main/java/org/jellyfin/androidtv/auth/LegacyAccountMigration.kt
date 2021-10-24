package org.jellyfin.androidtv.auth

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import timber.log.Timber

class LegacyAccountMigration(
	private val context: Context,
	private val authenticationStore: AuthenticationStore,
	private val accountManagerHelper: AccountManagerHelper,
	private val systemPreferences: SystemPreferences,
) {
	companion object {
		const val LEGACY_CREDENTIAL_FILE = "org.jellyfin.androidtv.login.json"
	}

	suspend fun migrate() {
		val path = context.filesDir.resolve(LEGACY_CREDENTIAL_FILE)

		if (path.exists() && !systemPreferences[SystemPreferences.legacyCredentialsMigrated]) {
			Timber.d("Starting migration of legacy credentials from $path")
			val json = Json {
				serializersModule = SerializersModule {
					contextual(UUIDSerializer())
				}
				ignoreUnknownKeys = true
			}
			val root = json.parseToJsonElement(path.readText()).jsonObject
			val server = root["serverInfo"]?.jsonObject
			val user = root["userDto"]?.jsonObject

			val serverId = server?.get("Id")?.jsonPrimitive?.content?.toUUIDOrNull()
			if (serverId != null) {
				val serverName = server["Name"]?.jsonPrimitive?.content
				val serverAddress = server["Address"]?.jsonPrimitive?.content
				val serverVersion = server["Version"]?.jsonPrimitive?.content

				if (!authenticationStore.containsServer(serverId)) {
					Timber.i("Migrating server $serverId")
					authenticationStore.putServer(
						id = serverId,
						server = AuthenticationStoreServer(
							name = serverName ?: "",
							address = serverAddress ?: "",
							loginDisclaimer = null,
							version = serverVersion,
							lastRefreshed = 0,
						)
					)
				}

				val userId = user?.get("Id")?.jsonPrimitive?.content?.toUUIDOrNull()
				if (userId != null && !authenticationStore.containsUser(serverId, userId)) {
					Timber.i("Migrating user $userId")
					val name = user["Name"]?.jsonPrimitive?.content ?: userId.toString()
					val requirePassword = user["HasPassword"]?.jsonPrimitive?.booleanOrNull ?: true
					val imageTag = user["PrimaryImageTag"]?.jsonPrimitive?.content
					val accessToken = user["AccessToken"]?.jsonPrimitive?.content ?: server["AccessToken"]?.jsonPrimitive?.content

					authenticationStore.putUser(
						server = serverId,
						userId = userId,
						userInfo = AuthenticationStoreUser(
							name = name,
							requirePassword = requirePassword,
							imageTag = imageTag
						)
					)
					accountManagerHelper.putAccount(AccountManagerAccount(
						id = userId,
						server = serverId,
						name = name,
						accessToken = accessToken?.let {
							// Convert empty string to null
							if (it.isBlank()) null
							else it
						}
					))
				}
			}

			systemPreferences[SystemPreferences.legacyCredentialsMigrated] = true
		} else {
			Timber.d("Skipping migration of legacy credentials from $path (file does not exist)")
		}
	}
}
