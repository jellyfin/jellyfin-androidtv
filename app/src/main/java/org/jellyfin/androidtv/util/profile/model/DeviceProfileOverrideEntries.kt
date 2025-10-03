package org.jellyfin.androidtv.util.profile.model

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jellyfin.androidtv.util.profile.CodecProfileSerializer
import org.jellyfin.sdk.model.api.CodecProfile
import org.jellyfin.sdk.model.api.CodecType
import org.jellyfin.sdk.model.api.ContainerProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.ProfileCondition
import org.jellyfin.sdk.model.api.ProfileConditionType
import org.jellyfin.sdk.model.api.ProfileConditionValue
import org.jellyfin.sdk.model.api.TranscodingProfile
import timber.log.Timber

enum class DeviceIdentifier(val string: String) {
	MANUFACTURER("Manufacturer"),
	MODEL("Model"),
	DEVICE("Device"),
	PRODUCT("Product"),
	BRAND("Brand"),
	HARDWARE("Hardware"),
	SKU("SKU"),
	SOC_MODEL("SOCModel");
}

@Serializable
data class OverrideRule(
	@SerialName("Devices")
	val devices: List<Device>,
	@SerialName("Profiles")
	val profiles: Profiles
)

@Serializable
data class Device(
	@SerialName("Name")
	val name: String?,
	@SerialName("Identifiers")
	val identifiers: Map<String, String>
) {
	fun matchesCurrentDevice(): Boolean =
		identifiers.all { (key, value) ->
			val identifier = DeviceIdentifier.entries.find { it.string == key }
			when (identifier) {
				DeviceIdentifier.MANUFACTURER -> Build.MANUFACTURER.equals(value, true)
				DeviceIdentifier.MODEL -> Build.MODEL.equals(value, true)
				DeviceIdentifier.DEVICE -> Build.DEVICE.equals(value, true)
				DeviceIdentifier.PRODUCT -> Build.PRODUCT.equals(value, true)
				DeviceIdentifier.BRAND -> Build.BRAND.equals(value, true)
				DeviceIdentifier.HARDWARE -> Build.HARDWARE.equals(value, true)
				DeviceIdentifier.SKU -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
					Build.SKU.equals(value, true)

				DeviceIdentifier.SOC_MODEL -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
					Build.SOC_MODEL.equals(value, true)

				null -> {
					Timber.w("Unknown identifier key in profile_overrides: $key")
					false
				}
			}
		}
}

@Serializable
data class Profiles(
	@SerialName("TranscodingProfiles")
	val transcodingProfiles: List<TranscodingProfile>? = null,
	@SerialName("DirectPlayProfiles")
	val directPlayProfiles: List<DirectPlayProfile>? = null,
	@SerialName("CodecProfiles")
	val codecProfiles: List<@Serializable(CodecProfileSerializer::class) CodecProfile>? = null,
	@SerialName("ContainerProfiles")
	val containerProfiles: List<ContainerProfile>? = null
)


// Adds DTO objects add defaults for `applyConditions` and `isRequired`
@Serializable
data class CodecProfileDto(
	@SerialName("Type")
	val type: CodecType,
	@SerialName("Codec")
	val codec: String? = null,
	@SerialName("Container")
	val container: String? = null,
	@SerialName("SubContainer")
	val subContainer: String? = null,
	@SerialName("Conditions")
	val conditions: List<ProfileConditionDto>,
	@SerialName("ApplyConditions")
	val applyConditions: List<ProfileCondition>? = null
) {
	fun toCodecProfile(): CodecProfile =
		CodecProfile(
			type = type,
			codec = codec,
			container = container,
			subContainer = subContainer,
			conditions = conditions.map { it.toProfileCondition() },
			applyConditions = applyConditions ?: emptyList()
		)
}

@Serializable
data class ProfileConditionDto(
	@SerialName("Condition")
	val condition: ProfileConditionType,
	@SerialName("Property")
	val property: ProfileConditionValue,
	@SerialName("Value")
	val value: String? = null,
	@SerialName("IsRequired")
	val isRequired: Boolean? = null
) {
	fun toProfileCondition(): ProfileCondition =
		ProfileCondition(
			condition = condition,
			property = property,
			value = value,
			isRequired = isRequired ?: false
		)
}
