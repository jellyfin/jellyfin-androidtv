@file:JvmName("CoroutineUtils")

package org.jellyfin.androidtv.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository

fun <T : Any> runBlocking(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.runBlocking {
	block()
}

fun readCustomMessagesOnLifecycle(
	lifecycle: Lifecycle,
	customMessageRepository: CustomMessageRepository,
	listener: (message: CustomMessage) -> Unit,
) {
	customMessageRepository.message
		.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
		.onEach { if (it != null) listener(it) }
		.launchIn(lifecycle.coroutineScope)
}
