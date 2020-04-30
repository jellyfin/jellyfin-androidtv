package org.jellyfin.androidtv.model.repository

import org.jellyfin.apiclient.model.serialization.GsonJsonSerializer
import org.jellyfin.apiclient.model.serialization.IJsonSerializer

object SerializerRepository {
	val serializer: IJsonSerializer = GsonJsonSerializer()
}
