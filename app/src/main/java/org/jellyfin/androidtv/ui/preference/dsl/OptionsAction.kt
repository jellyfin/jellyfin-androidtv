package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import java.util.UUID

/**
 * Perform a custom action when activated.
 */
class OptionsAction(
	private val context: Context
) : OptionsItemMutable<Unit>() {
	@DrawableRes
	var icon: Int? = null
	var content: String? = null
	var onActivate: () -> Unit = {}

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resId: Int) {
		content = context.getString(resId)
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = Preference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			icon?.let { icon -> it.setIcon(icon) }
			it.title = title
			it.summary = content
			it.setOnPreferenceClickListener {
				onActivate()
				container()
				true
			}
		}

		container += {
			pref.isEnabled = dependencyCheckFun() && enabled
		}
	}
}

@OptionsDSL
fun OptionsCategory.action(init: OptionsAction.() -> Unit) {
	this += OptionsAction(context).apply { init() }
}
