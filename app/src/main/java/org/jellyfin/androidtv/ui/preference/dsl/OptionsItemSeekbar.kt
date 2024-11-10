package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.ui.preference.custom.DurationSeekBarPreference
import java.util.UUID

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

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = DurationSeekBarPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			// Max must be set before min because the setter of min checks if the value is below the current value of max
			it.max = max
			it.min = min
			it.seekBarIncrement = increment
			it.value = binder.get()
			it.showSeekBarValue = true
			it.valueFormatter = valueFormatter
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as Int)
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
fun OptionsCategory.seekbar(init: OptionsItemSeekbar.() -> Unit) {
	this += OptionsItemSeekbar(context).apply { init() }
}
