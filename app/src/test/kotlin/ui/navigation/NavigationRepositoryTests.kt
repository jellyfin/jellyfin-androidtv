package ui.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.androidtv.ui.navigation.Destination
import org.jellyfin.androidtv.ui.navigation.NavigationRepositoryImpl

private class HomeFragmentStub : Fragment()
private class DetailFragmentStub : Fragment()
private class OtherFragmentStub : Fragment()

private fun destination(fragment: kotlin.reflect.KClass<out Fragment>) = Destination.Fragment(
	fragment = fragment,
	arguments = mockk<Bundle>(relaxed = true),
)

class NavigationRepositoryTests : FunSpec({
	val home = destination(HomeFragmentStub::class)

	test("starts at the default destination and cannot go back") {
		val repository = NavigationRepositoryImpl(home)

		repository.canGoBack shouldBe false
		repository.hasBackStack shouldBe false
		repository.goBack() shouldBe false
	}

	test("navigating adds a level that back removes again") {
		val repository = NavigationRepositoryImpl(home)
		repository.navigate(destination(DetailFragmentStub::class))

		repository.canGoBack shouldBe true
		repository.hasBackStack shouldBe true

		repository.goBack() shouldBe true
		repository.canGoBack shouldBe false
		repository.hasBackStack shouldBe false
	}

	// The app can be opened straight into a detail screen through a launcher tile, a deep link or a
	// search result. Before the fix the history was empty in that state, canGoBack was false, the
	// back handler stayed disabled and the key press closed the app while a detail screen was shown.
	test("back returns to the default destination when the app opened into a deep screen") {
		val repository = NavigationRepositoryImpl(home)
		repository.reset(destination(DetailFragmentStub::class), clearHistory = true)

		repository.canGoBack shouldBe true
		repository.hasBackStack shouldBe false

		repository.goBack() shouldBe true

		repository.canGoBack shouldBe false
		repository.goBack() shouldBe false
	}

	test("resetting to the default destination leaves nothing to go back to") {
		val repository = NavigationRepositoryImpl(home)
		repository.navigate(destination(DetailFragmentStub::class))
		repository.reset(null, clearHistory = true)

		repository.canGoBack shouldBe false
		repository.hasBackStack shouldBe false
	}

	test("levels above a deep entry point unwind before returning to the default destination") {
		val repository = NavigationRepositoryImpl(home)
		repository.reset(destination(DetailFragmentStub::class), clearHistory = true)
		repository.navigate(destination(OtherFragmentStub::class))

		repository.hasBackStack shouldBe true

		// back to the deep entry point
		repository.goBack() shouldBe true
		repository.hasBackStack shouldBe false
		repository.canGoBack shouldBe true

		// and from there to the default destination
		repository.goBack() shouldBe true
		repository.canGoBack shouldBe false
	}

	test("replace swaps the current level instead of adding one") {
		val repository = NavigationRepositoryImpl(home)
		repository.navigate(destination(DetailFragmentStub::class))
		repository.navigate(destination(OtherFragmentStub::class), replace = true)

		repository.hasBackStack shouldBe true
		repository.goBack() shouldBe true
		repository.canGoBack shouldBe false
	}
})
