package org.jellyfin.androidtv.util

class MarkdownBuilder : Appendable, CharSequence {
	private val stringBuilder = StringBuilder()

	override val length: Int get() = stringBuilder.length
	override fun get(index: Int): Char = stringBuilder[index]
	override fun subSequence(startIndex: Int, endIndex: Int) = stringBuilder.subSequence(startIndex, endIndex)

	override fun append(value: CharSequence?): MarkdownBuilder {
		stringBuilder.append(value)
		return this
	}

	override fun append(value: CharSequence?, p1: Int, p2: Int): MarkdownBuilder {
		stringBuilder.append(value)
		return this
	}

	override fun append(value: Char): MarkdownBuilder {
		stringBuilder.append(value)
		return this
	}

	override fun toString(): String {
		return stringBuilder.toString()
	}
}

inline fun buildMarkdown(builderAction: MarkdownBuilder.() -> Unit): String {
	return MarkdownBuilder().apply(builderAction).toString()
}

fun MarkdownBuilder.appendSection(name: String? = null, depth: Int = 3, content: MarkdownBuilder.() -> Unit) {
	if (name != null) {
		appendLine("${"#".repeat(depth)} $name")
	}

	if (last().category != CharCategory.LINE_SEPARATOR) appendLine()
	content()
	appendLine()
}

fun MarkdownBuilder.appendItem(name: String, value: MarkdownBuilder.() -> Unit) {
	append("***$name***: ")
	value()
	appendLine("  ")
}

fun MarkdownBuilder.appendCodeBlock(language: String, code: String?) {
	if (last().category != CharCategory.LINE_SEPARATOR) appendLine()

	appendLine("```$language")
	appendLine(code ?: "<null>")
	appendLine("```")
}

fun MarkdownBuilder.appendValue(value: String?) {
	append("`", value ?: "<null>", "`")
}

fun MarkdownBuilder.appendDetails(summary: String? = null, content: MarkdownBuilder.() -> Unit) {
	appendLine()
	appendLine("<details>")
	if (summary != null) appendLine("<summary>${summary}</summary>")
	appendLine()
	content()
	appendLine()
	appendLine("</details>")
	appendLine()
}
