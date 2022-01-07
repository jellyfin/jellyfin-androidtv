package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import javax.net.ssl.SSLHandshakeException

private fun ApiClientException.getFriendlyMessage(context: Context): String? = when (cause) {
	is SSLHandshakeException -> context.getString(R.string.server_issue_ssl_handshake)
	else -> null
}

/**
 * Create a localized string with the message to show an end-user of the app.
 */
fun RecommendedServerIssue.getFriendlyMessage(context: Context): String = when (this) {
	is RecommendedServerIssue.MissingSystemInfo -> throwable?.let { throwable ->
		// Search for exceptions we want to display but the SDK does not have
		// an RecommendedServerIssue member for
		if (throwable is ApiClientException) throwable.getFriendlyMessage(context)
		else null
	} ?: context.getString(R.string.server_issue_unable_to_connect)

	is RecommendedServerIssue.InvalidProductName -> context.getString(R.string.server_issue_invalid_product)

	RecommendedServerIssue.MissingVersion -> context.getString(R.string.server_issue_missing_version)

	is RecommendedServerIssue.UnsupportedServerVersion -> context.getString(
		R.string.server_issue_unsupported_version,
		version.toString(),
	)

	is RecommendedServerIssue.OutdatedServerVersion -> context.getString(
		R.string.server_issue_outdated_version,
		version.toString(),
		ServerRepository.minimumServerVersion.toString()
	)

	is RecommendedServerIssue.SlowResponse -> context.getString(R.string.server_issue_timeout)
}

/**
 * Get the summary for a collection of issues. This is the most significant issue in the collection
 * passed trough [RecommendedServerIssue.getFriendlyMessage].
 */
@Suppress("MagicNumber")
fun Collection<RecommendedServerIssue>.getSummary(context: Context): String? = maxByOrNull {
	// Assign a "score" to each issue type so we can return the most important one
	when (it) {
		is RecommendedServerIssue.MissingSystemInfo -> 5
		is RecommendedServerIssue.UnsupportedServerVersion -> 4
		is RecommendedServerIssue.OutdatedServerVersion -> 3
		is RecommendedServerIssue.SlowResponse -> 2
		RecommendedServerIssue.MissingVersion -> 1
		is RecommendedServerIssue.InvalidProductName -> 0
	}
}?.getFriendlyMessage(context)
