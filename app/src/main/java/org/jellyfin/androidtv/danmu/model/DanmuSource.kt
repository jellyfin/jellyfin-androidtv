package org.jellyfin.androidtv.danmu.model

import kotlinx.serialization.Serializable

@Serializable
data class DanmuSource(

	/**
	 * 弹幕来源id
	 */
	public var source: String? = null,

	/**
	 * 弹幕来源名称
	 */
	public val sourceName: String? = null,

	/**
	 * 是否开启
	 */
	public val opened: Boolean? = false,

	/**
	 * 弹幕信息
	 */
	public val danmuEvents: List<DanmuEvent>? = null,
)
