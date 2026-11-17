package org.jellyfin.androidtv.util

import android.content.Context
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

val Context.locale: Locale
	get() = resources.configuration.locales[0]

@JvmOverloads
fun Context.getDateFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = DateTimeFormatter
	.ofLocalizedDateTime(style)
	.withLocale(locale)

@JvmOverloads
fun Context.getTimeFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = DateTimeFormatter
	.ofLocalizedTime(style)
	.withLocale(locale)
