package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import java.util.*
import kotlin.reflect.KClass

/**
 * Link to a different fragment.
 */
class OptionsAction(
	private val context: Context
) : OptionsItemMutable<Unit>() {
	@DrawableRes
	var icon: Int? = null
	var content: String? = null
	var key: String = UUID.randomUUID().toString()
	var fragment: KClass<out OptionsFragment>? = null
	var extras: Bundle = bundleOf()
	var clickListener: Preference.OnPreferenceClickListener? = null

	inline fun <reified T : OptionsFragment> withFragment(extraBundle: Bundle = bundleOf()) {
		fragment = T::class
		extras = extraBundle
	}

	fun setListener(preferenceClickListener: Preference.OnPreferenceClickListener) {
		clickListener = preferenceClickListener
	}

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setContent(@StringRes resId: Int) {
		content = context.getString(resId)
	}

	fun setItemKey(key: String) {
		this.key = key
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = Preference(context).also {
			it.isPersistent = false
			it.key = key
			it.fragment = fragment?.qualifiedName
			it.extras.putAll(extras)
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			icon?.let { icon -> it.setIcon(icon) }
			it.title = title
			it.summary = content
			if (clickListener != null)
				it.onPreferenceClickListener = clickListener
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
