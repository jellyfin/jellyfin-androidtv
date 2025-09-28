package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import java.util.UUID

class OptionsLanguageList(
	private val context: Context
) : OptionsItemMutable<String>() {
	var entries: Map<String, String> = emptyMap()
	private var listPreference: ListPreference? = null
	private var onEntriesUpdated: (() -> Unit)? = null

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}
	
	fun setOnEntriesUpdated(callback: () -> Unit) {
		onEntriesUpdated = callback
	}
	
	fun updateEntries(newEntries: Map<String, String>) {
		entries = newEntries
		listPreference?.let { pref ->
			pref.entryValues = entries.keys.toTypedArray()
			pref.entries = entries.values.toTypedArray()
			val currentValue = binder.get()
			pref.value = currentValue
		}
		onEntriesUpdated?.invoke()
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		listPreference = ListPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			it.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
			it.entryValues = entries.keys.toTypedArray()
			it.entries = entries.values.toTypedArray()
			it.value = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue.toString())
				it.value = binder.get()
				container()
				false
			}
		}

		container += {
			listPreference?.isEnabled = dependencyCheckFun() && enabled
		}
	}
}

@OptionsDSL
fun OptionsCategory.languageList(init: OptionsLanguageList.() -> Unit) {
	this += OptionsLanguageList(context).apply { init() }
}
