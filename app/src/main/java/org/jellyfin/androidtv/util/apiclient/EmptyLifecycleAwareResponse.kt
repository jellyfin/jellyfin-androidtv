package org.jellyfin.androidtv.util.apiclient

import androidx.lifecycle.Lifecycle
import org.jellyfin.apiclient.interaction.EmptyResponse

abstract class EmptyLifecycleAwareResponse(
	private val lifecycle: Lifecycle,
) : EmptyResponse() {
	val active get() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

	override fun triggerInnerResponse() {
		if (!active) return

		super.triggerInnerResponse()
	}

	override fun onResponse() {
		if (!active) return

		super.onResponse()
	}

	override fun onError(ex: Exception?) {
		if (!active) return

		super.onError(ex)
	}
}
