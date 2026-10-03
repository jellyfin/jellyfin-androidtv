package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.PersonKind

private const val ROLE_SEPARATOR = " / "

/**
 * The person kinds the server reports once per job, making someone credited for multiple jobs show up as
 * multiple people.
 */
private val mergeableCrewKinds = setOf(
	PersonKind.DIRECTOR,
	PersonKind.WRITER,
	PersonKind.PRODUCER,
)

/**
 * Combine the credits of a person holding multiple crew jobs into a single entry listing all their roles,
 * like "Director / Producer". Credits of a kind outside [mergeableCrewKinds] are never combined, so an actor
 * playing multiple characters keeps one entry per character. The combined entry keeps the position of the
 * first credit.
 */
fun Collection<BaseItemPerson>.mergeCrewRoles(): List<BaseItemPerson> {
	val people = mutableListOf<BaseItemPerson>()
	val mergedIndices = mutableMapOf<UUID, Int>()

	for (person in this) {
		val mergeable = person.type in mergeableCrewKinds
		val index = if (mergeable) mergedIndices[person.id] else null

		if (index == null) {
			if (mergeable) mergedIndices[person.id] = people.size
			people += person
		} else {
			people[index] = people[index].withAdditionalRole(person.role)
		}
	}

	return people
}

private fun BaseItemPerson.withAdditionalRole(additionalRole: String?) = copy(
	role = listOfNotNull(role, additionalRole)
		.filter(String::isNotBlank)
		.distinct()
		.joinToString(ROLE_SEPARATOR)
		.ifEmpty { null }
)
