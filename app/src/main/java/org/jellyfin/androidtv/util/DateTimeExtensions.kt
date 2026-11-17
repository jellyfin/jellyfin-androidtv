package org.jellyfin.androidtv.util

import android.content.Context
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@JvmOverloads
fun Context.getDateFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = DateTimeFormatter
	.ofLocalizedDateTime(style)
	.withLocale(resources.configuration.locales[0])

@JvmOverloads
fun Context.getTimeFormatter(
	style: FormatStyle = FormatStyle.SHORT
): DateTimeFormatter = DateTimeFormatter
	.ofLocalizedTime(style)
	.withLocale(resources.configuration.locales[0])
