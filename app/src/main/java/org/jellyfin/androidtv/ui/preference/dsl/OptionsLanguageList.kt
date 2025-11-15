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
	private var content: String? = null

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resId: Int) {
		content = context.getString(resId)
	}

	fun setOnEntriesUpdated(callback: () -> Unit) {
		onEntriesUpdated = callback
	}

	fun updateEntries(newEntries: Map<String, String>) {
		entries = newEntries
		listPreference?.let { pref ->
			pref.entryValues = entries.keys.toTypedArray()
			pref.entries = entries.values.toTypedArray()

			// Force refresh of the current value and its summary display
			val currentValue = binder.get()
			pref.value = currentValue

			// Manually set the summary - show description if available, otherwise show selected entry
			// This ensures the summary shows the proper language name instead of "Not set"
			pref.summary = content ?: (entries[currentValue] ?: entries[""])
		}
		onEntriesUpdated?.invoke()
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		// Use standard ListPreference instead of RichListPreference to avoid dialog crash
		listPreference = ListPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			// Don't use SimpleSummaryProvider - manage summary manually for dynamic entries
			it.summaryProvider = null
			it.entryValues = entries.keys.toTypedArray()
			it.entries = entries.values.toTypedArray()
			val currentValue = binder.get()
			it.value = currentValue
			// Set initial summary - show description if available, otherwise show selected entry
			it.summary = content ?: (entries[currentValue] ?: entries[""])
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue.toString())
				val updatedValue = binder.get()
				it.value = updatedValue
				// Update summary when value changes - show description if available, otherwise show selected entry
				it.summary = content ?: (entries[updatedValue] ?: entries[""])
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
