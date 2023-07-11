package org.jellyfin.androidtv.auth.store

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.AccountManagerMigration
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import timber.log.Timber
import java.util.UUID

/**
 * Storage for authentication related entities. Stores servers with users inside, including
 * access tokens.
 *
 * The data is stored in a JSON file located in the applications data directory.
 */
class AuthenticationStore(
	private val context: Context,
	private val accountManagerMigration: AccountManagerMigration,
) {
	private val storePath
		get() = context.filesDir.resolve("authentication_store.json")

	private val json = Json {
		encodeDefaults = true
		serializersModule = SerializersModule {
			contextual(UUIDSerializer())
		}
		ignoreUnknownKeys = true
	}

	private val store by lazy {
		load().toMutableMap()
	}

	private fun load(): Map<UUID, AuthenticationStoreServer> {
		// No store found
		if (!storePath.exists()) return emptyMap()

		// Parse JSON document
		val root = try {
			json.parseToJsonElement(storePath.readText()).jsonObject
		} catch (e: SerializationException) {
			Timber.e(e, "Unable to read JSON")
			JsonObject(emptyMap())
		}

		// Check for version
		return when (root["version"]?.jsonPrimitive?.intOrNull) {
			1 -> json.decodeFromJsonElement<Map<UUID, AuthenticationStoreServer>>(root["servers"]!!)
				// Add access tokens from account manager to stored users and save the migrated data
				.let { servers -> accountManagerMigration.migrate(servers) }
				.also { servers -> write(servers) }

			2 -> json.decodeFromJsonElement<Map<UUID, AuthenticationStoreServer>>(root["servers"]!!)

			null -> {
				Timber.e("Authentication Store is corrupt!")
				emptyMap()
			}

			else -> {
				Timber.e("Authentication Store is using an unknown version!")
				emptyMap()
			}
		}
	}

	private fun write(servers: Map<UUID, AuthenticationStoreServer>): Boolean {
		val root = JsonObject(mapOf(
			"version" to JsonPrimitive(2),
			"servers" to json.encodeToJsonElement(servers)
		))

		storePath.writeText(json.encodeToString(root))

		return true
	}

	private fun save(): Boolean {
		return write(store)
	}

	fun getServers(): Map<UUID, AuthenticationStoreServer> = store

	fun getUsers(server: UUID): Map<UUID, AuthenticationStoreUser>? = getServer(server)?.users

	fun getServer(serverId: UUID) = store[serverId]

	fun getUser(serverId: UUID, userId: UUID) = getUsers(serverId)?.get(userId)

	fun putServer(id: UUID, server: AuthenticationStoreServer): Boolean {
		store[id] = server
		return save()
	}

	fun putUser(server: UUID, userId: UUID, userInfo: AuthenticationStoreUser): Boolean {
		val serverInfo = store[server] ?: return false

		store[server] = serverInfo.copy(users = serverInfo.users.toMutableMap().apply { put(userId, userInfo) })

		return save()
	}

	/**
	 * Removes the server and stored users from the credential store.
	 */
	fun removeServer(server: UUID): Boolean {
		store.remove(server)
		return save()
	}

	fun removeUser(server: UUID, user: UUID): Boolean {
		val serverInfo = store[server] ?: return false

		store[server] = serverInfo.copy(users = serverInfo.users.toMutableMap().apply { remove(user) })

		return save()
	}
}
