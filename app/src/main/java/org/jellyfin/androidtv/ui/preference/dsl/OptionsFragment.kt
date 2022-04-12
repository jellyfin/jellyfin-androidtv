package org.jellyfin.androidtv.ui.preference.dsl

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jellyfin.preference.store.AsyncPreferenceStore
import org.jellyfin.preference.store.PreferenceStore

abstract class OptionsFragment : LeanbackPreferenceFragmentCompat() {
	abstract val screen: OptionsScreen

	/**
	 * Some screens show different content based on changes made in child fragments.
	 * Setting the [rebuildOnResume] property to true will automatically rebuild the screen
	 * when the fragment is resumed.
	 */
	protected open val rebuildOnResume = false

	/**
	 * Preference stores used in current screen. Fragment will automatically call the update and
	 * commit functions for all async stores.
	 */
	protected open val stores: Array<PreferenceStore<*, *>> = emptyArray()

	// Used to not build twice during onCreate()
	private var skippedInitialResume = false

	override fun onCreate(savedInstanceState: Bundle?) {
		// Refresh all data in async stores
		runBlocking {
			stores
				.filterIsInstance<AsyncPreferenceStore<*, *>>()
				.map { async { it.update() } }
				.awaitAll()
		}

		super.onCreate(savedInstanceState)
	}

	override fun onStop() {
		super.onStop()

		// Save all data in async stores
		runBlocking {
			stores
				.filterIsInstance<AsyncPreferenceStore<*, *>>()
				.map { async { it.commit() } }
				.awaitAll()
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceScreen = screen.build(preferenceManager)
	}

	protected fun rebuild() {
		screen.build(preferenceManager, preferenceScreen)
	}

	override fun onResume() {
		super.onResume()

		if (skippedInitialResume && rebuildOnResume) rebuild()

		skippedInitialResume = true
	}
}
