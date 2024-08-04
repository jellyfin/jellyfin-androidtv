package org.jellyfin.androidtv.util

import android.content.Context
import android.text.Spanned
import androidx.core.text.HtmlCompat
import org.jellyfin.androidtv.R

/**
 * Convert string with HTML to a [Spanned]. Uses the [HtmlCompat.FROM_HTML_MODE_COMPACT] flag.
 */
fun String.toHtmlSpanned(): Spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)

/**
 * Remove HTML tags from string and return the plain representation.
 */
fun String.stripHtml(): String = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

/**
 * Utility to get the string for the "Load channels" button in the Live TV guide.
 */
fun getLoadChannelsLabel(context: Context, startNumber: String? = null, endNumber: String? = null) = buildString {
	append(context.getString(R.string.lbl_load_channels))

	if (!startNumber.isNullOrBlank() && !endNumber.isNullOrBlank()) append("$startNumber - $endNumber")
}
