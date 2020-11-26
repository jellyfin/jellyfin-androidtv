package org.jellyfin.androidtv.data.source

import android.content.Context
import com.google.gson.Gson
import org.jellyfin.androidtv.data.model.LogonCredentials
import org.jellyfin.androidtv.util.toUUIDOrNull

const val CREDENTIALS_PATH = "org.jellyfin.androidtv.login.json"

@Deprecated("Method of retrieving credentials stored in a legacy format")
class CredentialsFileSource(
	context: Context,
	serializer: Gson
) : JsonFileSource<LogonCredentials>(
	context,
	CREDENTIALS_PATH,
	serializer,
	LogonCredentials::class.java
) {
	override fun read(): LogonCredentials? {
		var data = super.read()

		// Normalize UUID formats (add hyphens)
		if (data?.server?.id != null) data.server!!.id = data.server!!.id.toUUIDOrNull().toString()
		if (data?.user?.id != null) data = LogonCredentials(data.server, data.user!!.copy(id = data.user!!.id.toUUIDOrNull().toString()))

		return data
	}
}
