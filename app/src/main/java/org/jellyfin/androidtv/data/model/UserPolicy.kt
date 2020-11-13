package org.jellyfin.androidtv.data.model

/**
 * UserPolicy model to use locally in place of UserPolicy model in ApiClient.
 */
data class UserPolicy(
	val enableLiveTvAccess: Boolean = true,
	val enableLiveTvManagement: Boolean = true
)
