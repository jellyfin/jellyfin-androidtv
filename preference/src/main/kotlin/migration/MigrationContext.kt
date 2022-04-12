package org.jellyfin.preference.migration

import timber.log.Timber
import kotlin.math.max

class MigrationContext<E, V> {
	private val migrations = mutableListOf<Migration<E, V>>()
	private var highVersion: Int = -1

	/**
	 * Migration function to upgrade the preferences from older app versions.
	 *
	 * Migrations look like this:
	 * ```kotlin
	 * migration(toVersion = 1) {
	 * 	// Get a value
	 * 	it.getString("example", "default")
	 * 	// Set a value
	 * 	setString("example", "new value")
	 * }
	 *
	 * @param toVersion The new version to upgrade to
	 * @param body Actual migration code
	 */
	inline fun migration(toVersion: Int, noinline body: E.(V) -> Unit) = migration(Migration(toVersion, body))

	fun migration(definition: Migration<E, V>) {
		if (definition.toVersion > highVersion) highVersion = definition.toVersion
		migrations.add(definition)
	}

	/**
	 * Apply all defined migrations. Do not call manually.
	 * When [currentVersion] is -1 all migrations are skipped.
	 *
	 * @return The new version of the store. This is the highest "toVersion" number.
	 */
	fun applyMigrations(currentVersion: Int, executeMigration: (Migration<E, V>) -> Unit): Int {
		Timber.i("Requested migration from $currentVersion to $highVersion. Found ${migrations.size} migrations in total.")
		return when {
			// No migrations
			highVersion == -1 -> currentVersion
			// All migrations already applied
			currentVersion >= highVersion -> currentVersion
			// Skip migrations (fresh install)
			currentVersion == -1 -> highVersion
			// Run migrations
			else -> migrations
				// Filter out old migrations
				.filter { it.toVersion > currentVersion }
				// Execute in order
				.sortedBy { it.toVersion }
				// Call executor
				.forEach { executeMigration(it) }
				// Return highest version
				.let { max(highVersion, currentVersion) }
		}
	}

	data class Migration<E, V>(
		val toVersion: Int,
		val body: E.(V) -> Unit
	)
}
