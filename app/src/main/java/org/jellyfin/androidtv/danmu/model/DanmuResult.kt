package org.jellyfin.androidtv.danmu.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class DanmuResult(
	/**
	 * The user id.
	 */
	@SerialName("hasNext")
	public val hasNext: Boolean? = false,
	public val data: List<DanmuSource>? = null,
	public val extra: String? = null,
	public val nextTime: Long? = null,

	)
