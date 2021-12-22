package org.jellyfin.androidtv.data.repository

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class NetworkStatusImplTest : ShouldSpec({
	should("start in unknown network state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl(mockk(), this, mockk(relaxed = true), mockk(relaxed = true))
		instance.state.value shouldBe NetworkState.UNKNOWN
	}

	should("update to connected state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl(mockk(), this, mockk(relaxed = true), mockk(relaxed = true))
		instance.notifyNetworkAvailable()
		instance.state.value shouldBe NetworkState.CONNECTED
	}

	should("update to disconnected state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl(mockk(), this, mockk(relaxed = true), mockk(relaxed = true))
		instance.notifyNetworkDisconnect()
		instance.state.value shouldBe NetworkState.DISCONNECTED
	}
})
