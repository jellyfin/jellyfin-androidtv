package org.jellyfin.androidtv.util

import android.content.Context
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

class MarkdownRenderer(context: Context) {
	private val markwon = Markwon.builder(context)
		.usePlugin(HtmlPlugin.create())
		.build()

	/**
	 * Convert string with markdown and HTML to a [Spanned].
	 */
	fun toMarkdownSpanned(input: String): Spanned = markwon.toMarkdown(input)
}
