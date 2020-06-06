package org.jellyfin.androidtv.preferences.ui.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import java.util.*

class OptionsItemList(
	private val context: Context
) : OptionsItemMutable<String>() {
	var entries: Map<String, String> = emptyMap()

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	override fun build(category: PreferenceCategory) {
		ListPreference(context).also {
			category.addPreference(it)

			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
			it.entryValues = entries.keys.toTypedArray()
			it.entries = entries.values.toTypedArray()
			it.value = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue.toString())
				it.value = binder.get()

				// Always return false because we save it
				false
			}
		}
	}
}

@OptionsDSL
fun OptionsCategory.list(init: OptionsItemList.() -> Unit) {
	this += OptionsItemList(context).apply { init() }
}
