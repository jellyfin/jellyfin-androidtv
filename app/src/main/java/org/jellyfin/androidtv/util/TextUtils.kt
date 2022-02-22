package org.jellyfin.androidtv.util

import android.text.Spanned
import androidx.core.text.HtmlCompat

/**
 * Convert string with HTML to a [Spanned]. Uses the [HtmlCompat.FROM_HTML_MODE_COMPACT] flag.
 */
fun String.toHtmlSpanned(): Spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
