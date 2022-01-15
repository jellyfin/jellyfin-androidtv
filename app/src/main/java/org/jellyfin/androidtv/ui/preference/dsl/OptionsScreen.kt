package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import kotlin.properties.ReadOnlyProperty

class OptionsScreen(
	private val context: Context
) {
	private val nodes = mutableListOf<OptionsCategory>()
	var title: String? = null

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	/**
	 * Create a category.
	 */
	fun category(init: OptionsCategory.() -> Unit) {
		val category = OptionsCategory(context)
			.apply { init() }

		nodes.add(category)
	}

	/**
	 * Create a link inside of a category.
	 */
	@OptionsDSL
	fun link(init: OptionsLink.() -> Unit) {
		val category = OptionsCategory(context)
		category += OptionsLink(context).apply { init() }

		nodes.add(category)
	}

	/**
	 * Create an action inside of a category.
	 */
	@OptionsDSL
	fun action(init: OptionsAction.() -> Unit) {
		val category = OptionsCategory(context)
		category += OptionsAction(context).apply { init() }

		nodes.add(category)
	}

	/**
	 * Create androidx PreferenceScreen instance.
	 */
	fun build(
		preferenceManager: PreferenceManager,
		preferenceScreen: PreferenceScreen = preferenceManager.createPreferenceScreen(context)
	): PreferenceScreen {
		val container = OptionsUpdateFunContainer()
		return preferenceScreen.also {
			// Clear current preferences in re-used screen
			it.removeAll()

			it.isPersistent = false
			it.title = title
			nodes.forEach { node ->
				node.build(it, container)
			}
		}
	}
}

@OptionsDSL
fun OptionsFragment.optionsScreen(
	init: OptionsScreen.() -> Unit
) = ReadOnlyProperty<OptionsFragment, OptionsScreen> { _, _ ->
	OptionsScreen(requireContext()).apply { init() }
}
