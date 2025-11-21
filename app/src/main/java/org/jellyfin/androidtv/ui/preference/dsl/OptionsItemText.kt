package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceCategory
import java.util.UUID

class OptionsItemText(
	private val context: Context,
) : OptionsItemMutable<String>() {
	var preferenceKey: String? = null

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val key = preferenceKey ?: UUID.randomUUID().toString()
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

		val pref = EditTextPreference(context).also {
			it.isPersistent = preferenceKey != null
			it.key = key
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			if (preferenceKey != null) {
				it.text = sharedPreferences.getString(key, "") ?: ""
			}
			it.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
		}

		container += {
			pref.isEnabled = dependencyCheckFun() && enabled
		}
	}
}

@OptionsDSL
fun OptionsCategory.text(init: OptionsItemText.() -> Unit) {
	this += OptionsItemText(context).apply { init() }
}
