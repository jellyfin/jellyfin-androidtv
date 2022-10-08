package org.jellyfin.androidtv.util.apiclient

import androidx.lifecycle.Lifecycle
import org.jellyfin.apiclient.interaction.Response

abstract class LifecycleAwareResponse<T>(
	private val lifecycle: Lifecycle,
) : Response<T>() {
	val active get() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

	override fun triggerInnerResponse() {
		if (!active) return

		super.triggerInnerResponse()
	}

	override fun onResponse(response: T) {
		if (!active) return

		super.onResponse(response)
	}

	override fun onError(exception: Exception?) {
		if (!active) return

		super.onError(exception)
	}
}
