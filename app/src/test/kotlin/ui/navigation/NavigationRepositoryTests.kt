package org.jellyfin.androidtv.ui.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NavigationRepositoryTests : FunSpec({
	val home = Destination.Fragment(Fragment::class, mockk(relaxed = true))
	val details = Destination.Fragment(ListFragment::class, mockk(relaxed = true))
	val other = Destination.Fragment(ListFragment::class, mockk(relaxed = true))

	test("resetting to the default destination does not create a back stack entry") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)

		repository.canGoBack.shouldBeFalse()
		repository.goBack().shouldBeFalse()
	}

	test("resetting to a specific destination does not create a back stack entry") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(details, true)

		repository.canGoBack.shouldBeFalse()
		repository.goBack().shouldBeFalse()
	}

	test("resetting sets the current destination") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(details, true)

		repository.currentDestination.value shouldBeSameInstanceAs details
	}

	test("resetting to the default destination sets the current destination to the default") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)

		repository.currentDestination.value shouldBeSameInstanceAs home
	}

	test("resetting without clearing the history keeps the previous entries") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)
		repository.reset(details, false)

		repository.canGoBack.shouldBeTrue()
		repository.goBack().shouldBeTrue()
		repository.currentDestination.value shouldBeSameInstanceAs home
	}

	test("resetting with clearing the history removes the previous entries") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)
		repository.navigate(details)
		repository.reset(other, true)

		repository.canGoBack.shouldBeFalse()
		repository.goBack().shouldBeFalse()
		repository.currentDestination.value shouldBeSameInstanceAs other
	}

	test("navigating adds an entry to the back stack") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)
		repository.navigate(details)

		repository.canGoBack.shouldBeTrue()
		repository.goBack().shouldBeTrue()
		repository.canGoBack.shouldBeFalse()
		repository.currentDestination.value shouldBeSameInstanceAs home
	}

	test("navigating with replace does not add an entry to the back stack") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(null, true)
		repository.navigate(details)
		repository.navigate(other, replace = true)

		repository.canGoBack.shouldBeTrue()
		repository.goBack().shouldBeTrue()
		repository.currentDestination.value shouldBeSameInstanceAs home
	}

	test("the previous destination is kept while navigating deeper") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(details, true)
		repository.navigate(other)

		repository.goBack().shouldBeTrue()
		repository.currentDestination.value shouldBeSameInstanceAs details
		repository.goBack().shouldBeFalse()
		repository.currentDestination.value shouldBeSameInstanceAs details
	}

	test("going back emits a go back action") {
		val repository = NavigationRepositoryImpl(home)
		repository.reset(null, true)
		repository.navigate(details)

		repository.goBack()

		val action = runBlocking { repository.currentAction.first() }
		action shouldBe NavigationAction.GoBack
	}

	test("resetting emits a navigate fragment action") {
		val repository = NavigationRepositoryImpl(home)

		repository.reset(details, true)

		val action = runBlocking { repository.currentAction.first() }
		val navigate = action.shouldBeInstanceOf<NavigationAction.NavigateFragment>()
		navigate.destination shouldBeSameInstanceAs details
		navigate.clear.shouldBeTrue()
		navigate.replace.shouldBeFalse()
	}

	test("navigating emits a navigate fragment action") {
		val repository = NavigationRepositoryImpl(home)

		repository.navigate(details)

		val action = runBlocking { repository.currentAction.first() }
		val navigate = action.shouldBeInstanceOf<NavigationAction.NavigateFragment>()
		navigate.destination shouldBeSameInstanceAs details
		navigate.clear.shouldBeFalse()
		navigate.replace.shouldBeFalse()
	}
})
