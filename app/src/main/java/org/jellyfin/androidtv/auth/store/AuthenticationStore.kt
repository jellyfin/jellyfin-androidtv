package org.jellyfin.androidtv.auth.store

import android.content.Context
import androidx.core.util.AtomicFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jellyfin.androidtv.auth.model.AuthenticationStoreServer
import org.jellyfin.androidtv.auth.model.AuthenticationStoreUser
import org.jellyfin.sdk.model.serializer.UUIDSerializer
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/**
 * Storage for authentication related entities. Stores servers with users inside, including
 * access tokens.
 *
 * The data is stored in a JSON file located in the applications data directory.
 */
class AuthenticationStore(context: Context) {
	private val storePath = context.filesDir.resolve("authentication_store.json")
	private val storeFile = AtomicFile(storePath)

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

	@OptIn(ExperimentalSerializationApi::class)
	private fun load(): Map<UUID, AuthenticationStoreServer> {
		// Parse JSON document
		val root = try {
			storeFile.openRead().use { json.decodeFromStream<JsonObject>(it) }
		} catch (_: FileNotFoundException) {
			// No store found
			return emptyMap()
		} catch (e: IOException) {
			Timber.e(e, "Unable to read authentication store")
			return emptyMap()
		} catch (e: SerializationException) {
			Timber.e(e, "Unable to read JSON")
			return emptyMap()
		}

		// Check for version
		return when (root["version"]?.jsonPrimitive?.intOrNull) {
			// Migration was removed, clear stored servers
			1 -> {
				Timber.e("Migrating from version 1 is no longer possible")
				emptyMap()
			}

			// Current version, return as-is
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

	@OptIn(ExperimentalSerializationApi::class)
	private fun write(servers: Map<UUID, AuthenticationStoreServer>): Boolean {
		val root = buildJsonObject {
			put("version", 2)
			put("servers", json.encodeToJsonElement(servers))
		}

		val stream = try {
			storeFile.startWrite()
		} catch (e: IOException) {
			Timber.e(e, "Unable to start writing authentication store")
			return false
		}

		return try {
			json.encodeToStream(root, stream)

			stream.fd.sync()
			storeFile.finishWrite(stream)

			// Verify the file write completed
			if (storePath.resolveSibling("${storePath.name}.new").exists()) throw IOException("Unable to commit authentication store")

			true
		} catch (e: IOException) {
			storeFile.failWrite(stream)
			Timber.e(e, "Unable to write authentication store")
			false
		}
	}

	private fun save(): Boolean = synchronized(storeFile) {
		write(store)
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

		store[server] = serverInfo.copy(users = serverInfo.users + (userId to userInfo))

		return save()
	}

	/**
	 * Removes the server and stored users from the credential store.
	 */
	fun removeServer(server: UUID): Boolean {
		store -= server
		return save()
	}

	fun removeUser(server: UUID, user: UUID): Boolean {
		val serverInfo = store[server] ?: return false

		store[server] = serverInfo.copy(users = serverInfo.users - user)

		return save()
	}
}
