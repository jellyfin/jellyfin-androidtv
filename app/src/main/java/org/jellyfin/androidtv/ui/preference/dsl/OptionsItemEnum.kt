package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.ui.preference.custom.RichListPreference
import org.jellyfin.preference.Preference
import org.jellyfin.preference.PreferenceEnum
import org.jellyfin.preference.store.PreferenceStore
import java.util.UUID

class OptionsItemEnum<T : Enum<T>>(
	private val context: Context,
	private val clazz: Class<T>
) : OptionsItemMutable<T>() {
	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	// Add exact copy of the OptionsItemMutable.bind function so the correct
	// store getter and setter will be used.
	override fun bind(store: PreferenceStore<*, *>, preference: Preference<T>) = bind {
		get { store[preference] }
		set { store[preference] = it }
		default { store.getDefaultValue(preference) }
	}

	private fun getEntries(): Map<T, String> {
		return clazz.enumConstants?.mapNotNull { entry ->
			when {
				entry is PreferenceEnum && entry.hidden -> null
				entry is PreferenceEnum && entry.nameRes != -1 -> Pair(entry, context.getString(entry.nameRes))
				else -> Pair(entry, entry.name)
			}
		}?.toMap().orEmpty()
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val entries = getEntries()

		val pref = RichListPreference<T>(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			it.summaryProvider = RichListPreference.SimpleSummaryProvider.instance
			it.setItems(entries)
			it.value = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				@Suppress("UNCHECKED_CAST")
				binder.set(newValue as? T ?: binder.default())
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
fun <T : Enum<T>> OptionsCategory.enum(
	clazz: Class<T>,
	init: OptionsItemEnum<T>.() -> Unit
) {
	this += OptionsItemEnum(context, clazz).apply { init() }
}

@OptionsDSL
inline fun <reified T : Enum<T>> OptionsCategory.enum(
	noinline init: OptionsItemEnum<T>.() -> Unit
) = enum(T::class.java, init)
