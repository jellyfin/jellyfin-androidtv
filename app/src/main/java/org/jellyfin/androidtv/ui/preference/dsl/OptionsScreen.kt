package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen

class OptionsScreen(
	private val context: Context
) {
	private val nodes = mutableListOf<OptionsCategory>()
	var title: String? = null

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun category(init: OptionsCategory.() -> Unit) {
		val category = OptionsCategory(context)
			.apply { init() }

		nodes.add(category)
	}

	/**
	 * Create androidx PreferenceScreen instance.
	 */
	fun build(preferenceManager: PreferenceManager): PreferenceScreen {
		val container = OptionsUpdateFunContainer()
		return preferenceManager.createPreferenceScreen(context).also {
			it.isPersistent = false
			it.title = title
			nodes.forEach { node ->
				node.build(it, container)
			}
		}
	}
}

@OptionsDSL
fun OptionsFragment.optionsScreen(init: OptionsScreen.() -> Unit) = lazy {
	OptionsScreen(requireContext())
		.apply { init() }
}
