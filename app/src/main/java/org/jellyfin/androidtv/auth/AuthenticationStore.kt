package org.jellyfin.androidtv.auth

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import timber.log.Timber
import java.util.*

/**
 * Storage for authentication related entities. Stores servers with users inside. Should be used in
 * combination with the [AccountManagerHelper] to store access tokens and integrate with the
 * operating system.
 *
 * The data is stored in a JSON file located in the applications data directory.
 */
class AuthenticationStore(
	private val context: Context
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

		// Check for version
		val root = json.parseToJsonElement(storePath.readText()).jsonObject
		return when (root["version"]?.jsonPrimitive?.intOrNull) {
			1 -> json.decodeFromJsonElement<Map<UUID, AuthenticationStoreServer>>(root["servers"]!!)
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

	private fun save(): Boolean {
		val root = JsonObject(mapOf(
			"version" to JsonPrimitive(1),
			"servers" to json.encodeToJsonElement(store)
		))

		storePath.writeText(json.encodeToString(root))

		return true
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

	fun containsServer(server: UUID): Boolean = server in store

	fun containsUser(server: UUID, user: UUID): Boolean = server in store && user in store[server]!!.users

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
