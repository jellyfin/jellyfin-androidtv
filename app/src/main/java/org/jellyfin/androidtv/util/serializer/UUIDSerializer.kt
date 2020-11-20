// FIXME File borrowed from the next version of the apiclient
package org.jellyfin.androidtv.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

/**
 * A UUID serializer that supports the GUIDs without dashes from the Jellyfin API
 */
object UUIDSerializer : KSerializer<UUID> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): UUID {
		val uuid = decoder.decodeString()

		return if (uuid.length == 32) UUID.fromString(uuid.replace(UUID_REGEX, "$1-$2-$3-$4-$5"))
		else UUID.fromString(uuid)
	}

	override fun serialize(encoder: Encoder, value: UUID) {
		encoder.encodeString(value.toString())
	}

	private val UUID_REGEX = "^([a-z\\d]{8})([a-z\\d]{4})(4[a-z\\d]{3})([a-z\\d]{4})([a-z\\d]{12})\$".toRegex()
}
