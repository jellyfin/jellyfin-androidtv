package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import java.util.UUID

class OptionsItemCheckbox(
	private val context: Context
) : OptionsItemMutable<Boolean>() {
	enum class Type {
		CHECKBOX,
		SWITCH
	}

	var contentOn: String? = null
	var contentOff: String? = null
	var type: Type = Type.CHECKBOX

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resIdOn: Int, @StringRes resIdOff: Int) {
		contentOn = context.getString(resIdOn)
		contentOff = context.getString(resIdOff)
	}

	/**
	 * Set the content that will always show. The checked state does not matter,
	 */
	fun setContent(@StringRes resId: Int) = setContent(resId, resId)

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = when (type) {
			Type.CHECKBOX -> CheckBoxPreference(context)
			Type.SWITCH -> SwitchPreference(context)
		}.also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.summaryOn = contentOn
			it.summaryOff = contentOff
			it.isChecked = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as Boolean)
				it.isChecked = binder.get()
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
fun OptionsCategory.checkbox(init: OptionsItemCheckbox.() -> Unit) {
	this += OptionsItemCheckbox(context).apply { init() }
}
