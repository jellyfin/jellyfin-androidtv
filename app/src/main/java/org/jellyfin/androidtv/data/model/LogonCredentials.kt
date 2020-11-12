package org.jellyfin.androidtv.data.model

import com.google.gson.annotations.SerializedName
import org.jellyfin.androidtv.util.apiclient.toLegacyServer
import org.jellyfin.androidtv.util.apiclient.toUser
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto

@Deprecated("Used for reading legacy credentials file")
class LogonCredentials {
	@SerializedName("serverInfo")
	var server: LegacyServer? = null
		private set

	@SerializedName("userDto")
	var user: User? = null
		private set

	constructor(server: LegacyServer?, user: User?) {
		this.server = server
		this.user = user
	}

	constructor(server: Server?, user: User?) {
		if (server != null) {
			this.server = LegacyServer(
				id = server.id,
				name = server.name,
				address = server.address,
				dateLastAccessed = server.dateLastAccessed
			)
		}
		this.user = user
	}

	constructor(serverInfo: ServerInfo?, userDto: UserDto?) {
		server = serverInfo?.toLegacyServer()
		user = userDto?.toUser()
	}
}
