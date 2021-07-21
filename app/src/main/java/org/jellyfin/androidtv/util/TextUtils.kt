package org.jellyfin.androidtv.util

import android.text.Spanned
import androidx.core.text.HtmlCompat
import java.util.*

/**
 * Convert string with HTML to a [Spanned]. Uses the [FROM_HTML_MODE_COMPACT] flag.
 */
fun String.toHtmlSpanned(): Spanned = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)
