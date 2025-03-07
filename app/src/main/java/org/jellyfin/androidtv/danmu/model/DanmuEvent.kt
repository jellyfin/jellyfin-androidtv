package org.jellyfin.androidtv.danmu.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DanmuEvent(
	@SerialName("m")
	private var content: String? = null,
	@SerialName("p")
	private var originInfo: String? = null,

) {

	private var startTimeMillis: Long = 0
	/**
	 * 弹幕类型 1 2 3:普通弹幕 4:底部弹幕 5:顶部弹幕 6:逆向弹幕 7:高级弹幕 8:代码弹幕 9:BAS弹幕(pool必须为2)
	 */
	private var position: Int = 0
	private var fontSize: Int = 0
	private val source: String? = null
	private var color: Int = 0

	fun getPosition(): Int {
		return this.position
	}

	fun getFontSize(): Int {
		return this.fontSize
	}

	fun getStartTimeMillis(): Long {
		return this.startTimeMillis
	}

	fun getColor(): Int {
		return this.color
	}

	fun getContent(): String? {
		return content
	}

	fun setContent(content: String?) {
		this.content = content
	}

	fun getOriginInfo(): String? {
		return originInfo
	}

	fun setOriginInfo(originInfo: String?) {
		this.originInfo = originInfo
	}

	public fun convert() {
		if (originInfo == null) {
			return
		}
		val split = originInfo!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		this.startTimeMillis = (split[0].toDouble() * 1000L).toLong()
		if (split.size <= 4) {
			return
		}

		this.position = split[1].toInt()
		this.fontSize = split[2].toInt()
		this.color = (split[3].toLong() or 0xff000000L).toInt()
	}
}
