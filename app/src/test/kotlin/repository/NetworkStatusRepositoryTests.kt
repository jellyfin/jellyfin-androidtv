package org.jellyfin.androidtv.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.constant.NetworkState

import org.jellyfin.androidtv.data.repository.NetworkStatusRepositoryImpl

class NetworkStatusRepositoryTests : FunSpec({
	test("starts in unknown network state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl(this, mockk(relaxed = true), mockk(relaxed = true))
		instance.state.value shouldBe NetworkState.UNKNOWN
	}

	test("updates to connected state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl(this, mockk(relaxed = true), mockk(relaxed = true))
		instance.notifyNetworkAvailable()
		instance.state.value shouldBe NetworkState.CONNECTED
	}

	test("updates to disconnected state").config(testCoroutineDispatcher = true) {
		val instance =
			NetworkStatusRepositoryImpl( this, mockk(relaxed = true), mockk(relaxed = true))
		instance.notifyNetworkDisconnect()
		instance.state.value shouldBe NetworkState.DISCONNECTED
	}
})
