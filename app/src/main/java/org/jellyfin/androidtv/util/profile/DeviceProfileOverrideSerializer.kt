package org.jellyfin.androidtv.util.profile

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jellyfin.androidtv.util.profile.model.CodecProfileDto
import org.jellyfin.sdk.model.api.CodecProfile

// Map DTO profiles to profiles during deserialization
object CodecProfileSerializer : KSerializer<CodecProfile> {
	private val dtoSerializer = CodecProfileDto.serializer()
	override val descriptor: SerialDescriptor = dtoSerializer.descriptor

	override fun deserialize(decoder: Decoder): CodecProfile {
		val dto = dtoSerializer.deserialize(decoder)
		return dto.toCodecProfile()
	}

	override fun serialize(encoder: Encoder, value: CodecProfile) {
		// Overrides are never serialized
	}
}
