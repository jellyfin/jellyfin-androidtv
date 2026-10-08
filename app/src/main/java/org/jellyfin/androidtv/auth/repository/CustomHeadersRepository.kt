package org.jellyfin.androidtv.auth.repository

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jellyfin.androidtv.auth.store.AuthenticationStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the custom HTTP headers (e.g. an API key or a reverse-proxy / identity-aware gateway
 * auth token) that should be attached to outgoing requests, matched by the request host.
 *
 * Headers of persisted servers are read from the [AuthenticationStore]. Because a server is only
 * persisted once it has been successfully reached, [setPendingHeaders] allows registering headers
 * for a server that is still being added so that they are also sent during connection probing.
 */
class CustomHeadersRepository(
	private val authenticationStore: AuthenticationStore,
) {
	private val pendingHeaders = ConcurrentHashMap<String, Map<String, String>>()

	/**
	 * Register [headers] to be sent to [host] while a server is being added. Passing empty headers
	 * clears any previously registered pending headers for the host.
	 */
	fun setPendingHeaders(host: String, headers: Map<String, String>) {
		if (headers.isEmpty()) pendingHeaders.remove(host)
		else pendingHeaders[host] = headers
	}

	fun clearPendingHeaders(host: String) {
		pendingHeaders.remove(host)
	}

	/**
	 * Return the custom headers to attach to a request to [url], or an empty map when none apply.
	 * Persisted servers take precedence over pending (in-progress add) headers.
	 */
	fun headersFor(url: HttpUrl): Map<String, String> {
		val host = url.host

		val stored = authenticationStore.getServers().values
			.firstOrNull { it.customHeaders.isNotEmpty() && it.address.toHttpUrlOrNull()?.host == host }
			?.customHeaders
		if (stored != null) return stored

		return pendingHeaders[host].orEmpty()
	}
}
