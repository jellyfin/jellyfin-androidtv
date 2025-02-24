package org.jellyfin.androidtv.danmu.model

import kotlinx.serialization.Serializable

@Serializable
data class DanmuParams(
	val needSites:Collection<String>,
)
