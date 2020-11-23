package org.jellyfin.androidtv.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.androidtv.util.serializer.UUIDSerializer
import timber.log.Timber
import java.io.File
import java.util.*

class AuthenticationStore {
	private val storePath
		get() = File("authentication_store.json")

	private val json = Json {
		encodeDefaults = true
		serializersModule = SerializersModule {
			contextual(UUIDSerializer)
		}
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
			1 -> parseV1(root)
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

	private fun parseV1(root: JsonObject) = json.decodeFromJsonElement<Map<UUID, AuthenticationStoreServer>>(root["servers"]!!)

	private fun save(): Boolean {
		val root = JsonObject(mapOf(
			"version" to JsonPrimitive(1),
			"servers" to json.encodeToJsonElement(store)
		))

		storePath.writeText(json.encodeToString(root))

		return false
	}

	fun getServers(): Map<UUID, AuthenticationStoreServer> = store

	fun getUsers(server: UUID): Map<UUID, AuthenticationStoreUser>? = getServers()[server]?.users

	fun putServer(id: UUID, server: AuthenticationStoreServer): Boolean {
		store[id] = server
		return save()
	}

	fun putUser(server: UUID, userId: UUID, userInfo: AuthenticationStoreUser): Boolean {
		val serverInfo = store[server] ?: return false

		store[server] = serverInfo.copy(users = serverInfo.users.toMutableMap().apply { put(userId, userInfo) })

		return save()
	}

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
