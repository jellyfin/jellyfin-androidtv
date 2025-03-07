package org.jellyfin.androidtv.customer

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import java.io.InputStream

class ByteReadChannelInputStream(private val channel: ByteReadChannel) : InputStream() {
	private var buffer: ByteArray? = null

	override fun read(): Int {
		buffer = buffer ?: ByteArray(1) // 初始化缓冲区
		return runBlocking {
			channel.readAvailable(buffer!!, 0, 1).also { read ->
				if (read > 0) {
					// 因为读取单个字节，所以只取第一个字节
					return@runBlocking buffer!![0].toInt() and 0xff
				}
			} ?: -1
		} // 返回-1表示EOF
	}

	override fun read(b: ByteArray, off: Int, len: Int): Int {
		return runBlocking { channel.readAvailable(b, off, len) }
	}

	override fun close() {
		channel.closedCause
	}
}
