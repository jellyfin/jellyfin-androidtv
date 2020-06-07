package org.jellyfin.androidtv.preferences.ui.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.preferences.ui.preference.DurationSeekBarPreference
import java.util.*

class OptionsItemSeekbar(
	private val context: Context
) : OptionsItemMutable<Int>() {

	var content: String? = null
	var min: Int = 0
	var max: Int = 100
	var increment: Int = 1
	var valueFormatter: DurationSeekBarPreference.ValueFormatter = DurationSeekBarPreference.ValueFormatter()

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resId: Int) {
		content = context.getString(resId)
	}

	override fun build(category: PreferenceCategory) {
		DurationSeekBarPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.min = min
			it.max = max
			it.seekBarIncrement = increment
			it.value = binder.get()
			it.showSeekBarValue = true
			it.valueFormatter = valueFormatter
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as Int)
				it.value = binder.get()

				// Always return false because we save it
				false
			}
		}
	}
}

@OptionsDSL
fun OptionsCategory.seekbar(init: OptionsItemSeekbar.() -> Unit) {
	this += OptionsItemSeekbar(context).apply { init() }
}
