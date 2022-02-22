package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import java.util.UUID

class OptionsItemList(
	private val context: Context
) : OptionsItemMutable<String>() {
	var entries: Map<String, String> = emptyMap()

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = ListPreference(context).also {
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

				// Always return false because we save it
				false
			}
		}

		container += {
			pref.isEnabled = dependencyCheckFun() && enabled
		}
	}
}

@OptionsDSL
fun OptionsCategory.list(init: OptionsItemList.() -> Unit) {
	this += OptionsItemList(context).apply { init() }
}
