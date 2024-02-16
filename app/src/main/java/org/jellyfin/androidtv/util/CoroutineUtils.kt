@file:JvmName("CoroutineUtils")

package org.jellyfin.androidtv.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.repository.CustomMessageRepository

fun <T : Any> runOnLifecycle(
	lifecycle: Lifecycle,
	block: suspend CoroutineScope.() -> T
) = lifecycle.coroutineScope.launch { block() }

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
