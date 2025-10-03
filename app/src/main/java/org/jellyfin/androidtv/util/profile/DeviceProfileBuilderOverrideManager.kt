package org.jellyfin.androidtv.util.profile

import org.jellyfin.sdk.model.api.CodecProfile
import org.jellyfin.sdk.model.api.ContainerProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.ProfileCondition
import org.jellyfin.sdk.model.api.TranscodingProfile
import org.jellyfin.sdk.model.deviceprofile.DeviceProfileBuilder
import timber.log.Timber

/// Rudimentary override handling for device profile
// Consider moving this override logic into the SDK -- DeviceProfileBuilder should handle this
// TODO Handle partially overlapping profiles by separating into their own distinct profile
object DeviceProfileBuilderOverrideManager {
    fun applyOverrideProfiles(builder: DeviceProfileBuilder, vararg overrides: CodecProfile) =
        applyOverrideProfiles(
            overrides,
            builder.codecProfiles,
            match = { existing, override ->
                existing.type == override.type &&
                        existing.codec == override.codec &&
                        existing.container == override.container &&
                        existing.subContainer == override.subContainer
            },
            conditions = { existing, override ->
                removeConflictingConditions(existing.conditions, override.conditions)
                removeConflictingConditions(existing.applyConditions, override.applyConditions)

                existing.conditions.isEmpty() && existing.applyConditions.isEmpty()
            }
        )

    fun applyOverrideProfiles(builder: DeviceProfileBuilder, vararg overrides: TranscodingProfile) =
        applyOverrideProfiles(
            overrides,
            builder.transcodingProfiles,
            match = { existing, override -> existing.type == override.type }
        )

    fun applyOverrideProfiles(builder: DeviceProfileBuilder, vararg overrides: DirectPlayProfile) =
        applyOverrideProfiles(
            overrides,
            builder.directPlayProfiles,
            match = { existing, override -> existing.type == override.type }
        )

    fun applyOverrideProfiles(builder: DeviceProfileBuilder, vararg overrides: ContainerProfile) =
        applyOverrideProfiles(
            overrides,
            builder.containerProfiles,
            match = { existing, override -> existing.type == override.type },
            conditions = { existing, override ->
                removeConflictingConditions(existing.conditions, override.conditions)

                existing.conditions.isEmpty()
            }
        )

    private inline fun <T> applyOverrideProfiles(
        overrides: Array<out T>,
        profiles: MutableCollection<T>,
        crossinline match: (T, T) -> Boolean,
        crossinline conditions: (T, T) -> Boolean = { _, _ -> true }
    ) {
        try {
            overrides.forEach { override ->
                profiles.removeIf { existing ->
                    if (!match(existing, override)) return@removeIf false
                    if (!conditions(existing, override)) return@removeIf false

                    true
                }

                profiles.add(override)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply override profiles")
        }
    }

    // Remove condition values that conflict with override conditions
    private fun removeConflictingConditions(
        existing: List<ProfileCondition>,
        overrides: List<ProfileCondition>
    ) {
        overrides.forEach { override ->
            val iterator = (existing as MutableList).listIterator()

            for (current in iterator.asSequence()) {
                if (current.property != override.property) continue

                removeOverlappingValues(current.value, override.value)?.let { newValue ->
                    iterator.set(current.copy(value = newValue))
                } ?: iterator.remove()
            }
        }
    }

    // Remove values that conflict with override values
    private fun removeOverlappingValues(
        existingValue: String?,
        overrideValue: String?,
        delimiter: String = "|"
    ): String? {
        if (existingValue.isNullOrBlank() || overrideValue.isNullOrBlank()) return existingValue

        val existingValues = existingValue.split(delimiter)
        val overrideValues = overrideValue.split(delimiter)

        val remaining = existingValues.filterNot { it in overrideValues }

        return remaining.joinToString(delimiter).ifEmpty { null }
    }
}
