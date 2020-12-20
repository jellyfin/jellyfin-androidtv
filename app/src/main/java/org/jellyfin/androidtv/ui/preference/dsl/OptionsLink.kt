package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import java.util.*
import kotlin.reflect.KClass

/**
 * Link to a different fragment.
 */
class OptionsLink(
	private val context: Context
) : OptionsItemMutable<Unit>() {
	@DrawableRes
	var icon: Int? = null
	var content: String? = null
	var fragment: KClass<out OptionsFragment>? = null

	inline fun <reified T : OptionsFragment> withFragment() {
		fragment = T::class
	}

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
			it.fragment = fragment?.qualifiedName
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			icon?.let { icon -> it.setIcon(icon) }
			it.title = title
			it.summary = content
		}

		container += {
			pref.isEnabled = dependencyCheckFun() && enabled
		}
	}
}

@OptionsDSL
fun OptionsCategory.link(init: OptionsLink.() -> Unit) {
	this += OptionsLink(context).apply { init() }
}
