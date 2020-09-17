package org.jellyfin.androidtv.data.model

import com.google.gson.annotations.SerializedName
import org.jellyfin.androidtv.util.apiclient.toServer
import org.jellyfin.androidtv.util.apiclient.toUser
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto

@Deprecated("Used for reading legacy credentials file")
class LogonCredentials {
	@SerializedName("serverInfo")
	var server: Server? = null
		private set

	@SerializedName("userDto")
	var user: User? = null
		private set

	constructor(server: Server?, user: User?) {
		this.server = server
		this.user = user
	}

	constructor(serverInfo: ServerInfo?, userDto: UserDto?) {
		server = serverInfo?.toServer()
		user = userDto?.toUser()
	}
}
