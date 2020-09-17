package org.jellyfin.androidtv.data.model

/**
 * UserConfiguration model to use locally in place of UserConfiguration model in ApiClient.
 */
data class UserConfiguration(
	val latestItemsExcludes: Array<String> = emptyArray()
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as UserConfiguration

		if (!latestItemsExcludes.contentEquals(other.latestItemsExcludes)) return false

		return true
	}

	override fun hashCode() = latestItemsExcludes.contentHashCode()
}
