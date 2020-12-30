package org.jellyfin.androidtv.util

import android.os.Build
import android.text.Html
import android.text.Spanned
import java.util.*

fun String.toHtmlSpanned(): Spanned {
	@Suppress("DEPRECATION")
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
		Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT)
	else
		Html.fromHtml(this)
}

private val UUID_REGEX = "^([a-z\\d]{8})([a-z\\d]{4})(4[a-z\\d]{3})([a-z\\d]{4})([a-z\\d]{12})\$".toRegex()

/**
 * Parse string a UUID. Supports the simple and hyphenated formats.
 *
 * ```kotlin
 * "936DA01F9ABD4d9d80C702AF85C822A8".toUUIDOrNull() // OK
 * "550e8400-e29b-41d4-a716-446655440000".toUUIDOrNull() // OK
 * "notavaliduuid".toUUIDOrNull() // NOT OK
 * ```
 */
fun String.toUUIDOrNull(): UUID? = try {
	if (length == 32) UUID.fromString(this.replace(UUID_REGEX, "$1-$2-$3-$4-$5"))
	else UUID.fromString(this)
} catch (_: IllegalArgumentException) {
	null
}

/**
 * @see [toUUIDOrNull]
 */
fun String.toUUID(): UUID = toUUIDOrNull()!!
