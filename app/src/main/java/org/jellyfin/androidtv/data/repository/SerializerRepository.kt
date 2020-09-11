package org.jellyfin.androidtv.data.repository

import org.jellyfin.apiclient.serialization.GsonJsonSerializer

object SerializerRepository {
	val serializer = GsonJsonSerializer()
}
