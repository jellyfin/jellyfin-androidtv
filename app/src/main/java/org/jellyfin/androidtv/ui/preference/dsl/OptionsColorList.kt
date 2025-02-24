package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.ui.preference.custom.ColorListPreference
import org.jellyfin.androidtv.ui.preference.custom.ColorPickerDialogFragment
import java.util.UUID

class OptionsColorList(
	private val context: Context
) : OptionsItemMutable<Color>() {
	var entries: Map<Color, String> = emptyMap()

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = ColorListPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			it.summaryProvider = ColorListPreference.SimpleSummaryProvider.instance
			it.items = entries.map { entry -> ColorPickerDialogFragment.ColorListItem(entry.key, entry.value) }
			it.entryValues = entries.keys.map { color -> color.toArgb().toString() }.toTypedArray()
			it.entries = entries.values.toTypedArray()
			it.value = binder.get().toArgb().toString()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as Color)
				it.value = binder.get().toArgb().toString()
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
