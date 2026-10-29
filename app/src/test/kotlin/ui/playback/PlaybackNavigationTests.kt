package org.jellyfin.androidtv.ui.playback

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationAction
import org.jellyfin.androidtv.ui.navigation.NavigationRepositoryImpl
import org.jellyfin.androidtv.util.createBundle
import java.util.UUID

class PlaybackNavigationTests : BehaviorSpec({
	beforeSpec {
		mockkStatic("org.jellyfin.androidtv.util.BundleExtensionsKt")
		every { createBundle(any()) } returns mockk<Bundle>(relaxed = true)
	}
	afterSpec { unmockkStatic("org.jellyfin.androidtv.util.BundleExtensionsKt") }

	Given("navigation away from an active legacy player") {
		for (replacing in listOf(true, false)) {
			for (stillOwner in listOf(true, false)) {
				Then("${if (replacing) "replacement" else "push"} preserves its destination when the outgoing player owns the session: $stillOwner") {
					val f = NavigationFixture(stillOwner)
					val requested = mockk<Destination.Fragment>(relaxed = true)
					f.navigation.navigate(requested, replacing)
					f.flag(if (replacing) "mRemoving" else "mDetached")
					val action = f.action

					f.fragment.onStop()

					f.action shouldBe action
					f.navigation.canGoBack shouldBe true
					verify(exactly = if (stillOwner) 1 else 0) { f.controller.endPlayback() }
				}
			}
		}
	}
	Given("the existing ways of leaving playback") {
		Then("backgrounding still ends the owned session and returns to the previous screen") {
			val f = NavigationFixture()
			f.fragment.onStop()
			f.action shouldBe NavigationAction.GoBack
			f.navigation.canGoBack shouldBe false
			verify(exactly = 1) { f.controller.endPlayback() }
		}
		Then("an explicit close followed by stop does not navigate twice") {
			val f = NavigationFixture()
			f.fragment.closePlayer()
			f.fragment.onStop()
			f.action shouldBe NavigationAction.GoBack
			verify(exactly = 1) { f.controller.endPlayback() }
		}
		Then("closing without a previous screen still returns Home") {
			val f = NavigationFixture()
			f.navigation.goBack()
			f.fragment.onStop()
			f.action shouldBe NavigationAction.NavigateFragment(Destinations.home, true, false, false)
		}
		for (nextUp in listOf(true, false)) {
			Then("${if (nextUp) "NextUp" else "Still Watching"} keeps its destination when playback stops") {
				val f = NavigationFixture()
				every { f.fragment.showNextUp(any()) } answers { callOriginal() }
				every { f.fragment.showStillWatching(any()) } answers { callOriginal() }
				if (nextUp) f.fragment.showNextUp(UUID.randomUUID()) else f.fragment.showStillWatching(UUID.randomUUID())
				val action = f.action
				f.fragment.onStop()
				f.action shouldBe action
			}
		}
	}
})

/** Run the real stop/close methods and navigation stack without Android/Koin constructors. */
private class NavigationFixture(stillOwner: Boolean = true) {
	val fragment = spyk(mockk<CustomPlaybackOverlayFragment>(relaxed = true))
	val controller = mockk<PlaybackController>(relaxed = true)
	val navigation = NavigationRepositoryImpl(mockk(relaxed = true))
	val action get() = navigation.currentAction.replayCache.last()
	init {
		val container = PlaybackControllerContainer().apply { playbackController = controller }
		every { controller.fragment } returns if (stillOwner) fragment else mockk()
		every { fragment.onStop() } answers { callOriginal() }
		every { fragment.closePlayer() } answers { callOriginal() }
		every { fragment.isRemoving } answers { Fragment::class.java.getDeclaredField("mRemoving").apply { isAccessible = true }.getBoolean(fragment) }
		every { fragment.isDetached } answers { Fragment::class.java.getDeclaredField("mDetached").apply { isAccessible = true }.getBoolean(fragment) }
		for ((name, value) in listOf("playbackControllerContainer" to lazyOf(container), "navigationRepository" to lazyOf(navigation))) {
			CustomPlaybackOverlayFragment::class.java.getDeclaredField(name).apply { isAccessible = true }.set(fragment, value)
		}
		navigation.navigate(mockk<Destination.Fragment>(relaxed = true))
	}
	fun flag(name: String) {
		Fragment::class.java.getDeclaredField(name).apply { isAccessible = true }.setBoolean(fragment, true)
	}
}
