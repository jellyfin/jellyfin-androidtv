package org.jellyfin.androidtv.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.model.api.ForceKeepAliveMessage
import org.jellyfin.sdk.model.serializer.toUUID

class SdkTests : FunSpec({
	test("SDK deserialization works as expected") {
		val messageId = "b9724a318b0140088fa010e246eb3bb8"
		val json = """{"MessageId":"$messageId","Data":60,"MessageType":"ForceKeepAlive"}"""
		ApiSerializer.decodeSocketMessage(json) shouldBe ForceKeepAliveMessage(60, messageId.toUUID())
	}
})
