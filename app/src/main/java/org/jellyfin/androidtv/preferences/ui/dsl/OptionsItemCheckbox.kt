package org.jellyfin.androidtv.preferences.ui.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import java.util.*

class OptionsItemCheckbox(
	private val context: Context
) : OptionsItemMutable<Boolean>() {
	enum class Type {
		CHECKBOX,
		SWITCH
	}

	var content: String? = null
	var type: Type = Type.CHECKBOX

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resId: Int) {
		content = context.getString(resId)
	}

	override fun build(category: PreferenceCategory) {
		val pref = when (type) {
			Type.CHECKBOX -> CheckBoxPreference(context)
			Type.SWITCH -> SwitchPreference(context)
		}

		pref.also {
			category.addPreference(it)

			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.summary = content
			it.isChecked = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as Boolean)
				it.isChecked = binder.get()

				// Always return false because we save it
				false
			}
		}
	}
}

@OptionsDSL
fun OptionsCategory.checkbox(init: OptionsItemCheckbox.() -> Unit) {
	this += OptionsItemCheckbox(context).apply { init() }
}
