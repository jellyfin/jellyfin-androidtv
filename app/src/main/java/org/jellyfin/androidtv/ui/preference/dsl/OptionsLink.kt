package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import java.util.UUID
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
	var extras: Bundle? = null

	inline fun <reified T : OptionsFragment> withFragment(extraBundle: Bundle? = null) {
		fragment = T::class
		extras = extraBundle
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
			extras?.let { extras -> it.extras.putAll(extras)}
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
