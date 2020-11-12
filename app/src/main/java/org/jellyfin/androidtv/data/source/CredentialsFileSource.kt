package org.jellyfin.androidtv.data.source

import android.content.Context
import com.google.gson.Gson
import org.jellyfin.androidtv.data.model.LogonCredentials

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
)
