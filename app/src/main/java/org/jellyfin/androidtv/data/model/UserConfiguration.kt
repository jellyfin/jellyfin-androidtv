package org.jellyfin.androidtv.data.model

/**
 * UserConfiguration model to use locally in place of UserConfiguration model in ApiClient.
 */
data class UserConfiguration(
	val latestItemsExcludes: List<String> = emptyList()
)
