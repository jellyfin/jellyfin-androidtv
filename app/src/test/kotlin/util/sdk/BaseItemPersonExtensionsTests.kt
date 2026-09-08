package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.PersonKind
import java.util.UUID

private fun person(
	id: UUID,
	type: PersonKind,
	role: String?,
) = BaseItemPerson(
	id = id,
	name = "Person $id",
	type = type,
	role = role,
)

class BaseItemPersonExtensionsTests : FunSpec({
	val personId = UUID.randomUUID()
	val otherPersonId = UUID.randomUUID()

	test("mergeCrewRoles() combines the crew jobs of a single person") {
		val people = listOf(
			person(personId, PersonKind.DIRECTOR, "Director"),
			person(personId, PersonKind.PRODUCER, "Producer"),
		).mergeCrewRoles()

		people.size shouldBe 1
		people[0].role shouldBe "Director / Producer"
		people[0].type shouldBe PersonKind.DIRECTOR
	}

	test("mergeCrewRoles() keeps different people separate") {
		val people = listOf(
			person(personId, PersonKind.DIRECTOR, "Director"),
			person(otherPersonId, PersonKind.PRODUCER, "Producer"),
		).mergeCrewRoles()

		people.size shouldBe 2
		people[0].role shouldBe "Director"
		people[1].role shouldBe "Producer"
	}

	test("mergeCrewRoles() keeps a credit per character for actors") {
		val people = listOf(
			person(personId, PersonKind.ACTOR, "Ana"),
			person(personId, PersonKind.ACTOR, "Ana's twin"),
		).mergeCrewRoles()

		people.size shouldBe 2
	}

	test("mergeCrewRoles() keeps the position of the first credit") {
		val people = listOf(
			person(otherPersonId, PersonKind.ACTOR, "Ana"),
			person(personId, PersonKind.DIRECTOR, "Director"),
			person(otherPersonId, PersonKind.WRITER, "Screenplay"),
			person(personId, PersonKind.PRODUCER, "Producer"),
		).mergeCrewRoles()

		people.size shouldBe 3
		people[1].role shouldBe "Director / Producer"
		people[2].role shouldBe "Screenplay"
	}

	test("mergeCrewRoles() lists a repeated job once") {
		val people = listOf(
			person(personId, PersonKind.WRITER, "Writer"),
			person(personId, PersonKind.WRITER, "Writer"),
		).mergeCrewRoles()

		people.size shouldBe 1
		people[0].role shouldBe "Writer"
	}

	test("mergeCrewRoles() ignores missing job names") {
		val people = listOf(
			person(personId, PersonKind.DIRECTOR, null),
			person(personId, PersonKind.PRODUCER, "Producer"),
			person(otherPersonId, PersonKind.WRITER, null),
			person(otherPersonId, PersonKind.PRODUCER, null),
		).mergeCrewRoles()

		people.size shouldBe 2
		people[0].role shouldBe "Producer"
		people[1].role shouldBe null
	}
})
