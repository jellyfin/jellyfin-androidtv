package org.jellyfin.androidtv.model.repository

import org.jellyfin.apiclient.serialization.GsonJsonSerializer

object SerializerRepository {
	val serializer = GsonJsonSerializer()
}
