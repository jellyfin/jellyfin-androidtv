package org.jellyfin.androidtv.util

import java.time.LocalDateTime
import org.jellyfin.androidtv.preference.UserPreferences

internal fun calculateNextUpDateCutoff(
	retentionDays: Int,
	now: LocalDateTime = LocalDateTime.now(),
): LocalDateTime? = if (retentionDays <= 0) null else now.minusDays(retentionDays.toLong())

fun UserPreferences.nextUpDateCutoff(now: LocalDateTime = LocalDateTime.now()): LocalDateTime? =
	calculateNextUpDateCutoff(this[UserPreferences.nextUpRetentionDays], now)

