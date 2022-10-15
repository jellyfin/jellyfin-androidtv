package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.ui.preference.custom.ColorListPreference
import java.util.UUID

class OptionsColorList(
	private val context: Context
) : OptionsItemMutable<Long>() {
	var entries: Map<Long, String> = emptyMap()

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = ColorListPreference<String>(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			it.summaryProvider = ColorListPreference.SimpleSummaryProvider.instance
			it.setItems(entries)
			it.entryValues = entries.keys.map { longitems -> longitems.toString() }.toTypedArray()
			it.entries = entries.values.toTypedArray()
			it.value = binder.get().toString()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue.toString().toLong())
				it.value = binder.get().toString()
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
fun OptionsCategory.colorList(init: OptionsColorList.() -> Unit) {
	this += OptionsColorList(context).apply { init() }
}
