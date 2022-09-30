@file:JvmName("CoroutineUtils")

package org.jellyfin.androidtv.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository

fun <T : Any> runBlocking(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.runBlocking {
	block()
}

fun readCustomMessagesOnLifecycle(lifecycle: Lifecycle, customMessageRepository: CustomMessageRepository, listener: (message: CustomMessage) -> Unit) {
	lifecycle.coroutineScope.launch {
		lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
			customMessageRepository.message.collect {
				if (it != null) listener(it)
			}
		}
	}
}
