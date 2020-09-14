package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.SharedPreferenceStore
import java.util.*

class OptionsItemEnum<T : Enum<T>>(
	private val context: Context,
	private val clazz: Class<T>
) : OptionsItemMutable<T>() {
	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	// Add exact copy of the OptionsItemMutable.bind function so the correct
	// store getter and setter will be used.
	override fun bind(store: SharedPreferenceStore, preference: Preference<T>) = bind {
		get { store[preference] }
		set { store[preference] = it }
		default { store.getDefaultValue(preference) }
	}

	private fun getValueByString(value: String) = clazz.enumConstants?.first { it.name == value }

	private fun getEntries(): Map<String, String> {
		return clazz.enumConstants?.mapNotNull { entry ->
			val options = clazz
				.getDeclaredField(entry.name)
				.getAnnotation(EnumDisplayOptions::class.java)

			when {
				// Options set but entry is hidden
				options?.hidden == true -> null
				// Options not set or name not set
				options == null || options.name == -1 -> Pair(entry.name, entry.name)
				// Options set and name set
				else -> Pair(entry.name, context.getString(options.name))
			}
		}?.toMap().orEmpty()
	}

	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val entries = getEntries()

		val pref = ListPreference(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
			it.entryValues = entries.keys.toTypedArray()
			it.entries = entries.values.toTypedArray()
			it.value = binder.get().toString()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(getValueByString(newValue as String) ?: binder.default())
				it.value = binder.get().toString()
				container()

				// Always return false because we save it
				false
			}
		}

		container +=  {
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
