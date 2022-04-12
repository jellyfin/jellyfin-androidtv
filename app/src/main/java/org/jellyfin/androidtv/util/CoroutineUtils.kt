@file:JvmName("CoroutineUtils")
package org.jellyfin.androidtv.util

import kotlinx.coroutines.CoroutineScope

fun <T: Any> runBlocking(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.runBlocking {
	block()
}
