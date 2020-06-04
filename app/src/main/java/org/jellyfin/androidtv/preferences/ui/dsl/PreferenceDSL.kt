package org.jellyfin.androidtv.preferences.ui.dsl

import androidx.annotation.StringRes
import androidx.preference.*
import org.jellyfin.androidtv.preferences.ui.preference.ButtonRemapPreference
import org.jellyfin.androidtv.preferences.ui.preference.DurationSeekBarPreference
import java.util.*

@DslMarker
annotation class PreferenceDSL

@PreferenceDSL
inline fun <T : Preference> PreferenceGroup.appendPreference(
	preference: T,
	init: T.() -> Unit
) {
	preference.isPersistent = false

	addPreference(preference)

	// Use random UUID as key because we need something unique but don't actually save anything
	preference.key = UUID.randomUUID().toString()
	preference.init()
}

@PreferenceDSL
inline fun PreferenceScreen.category(
	@StringRes title: Int,
	init: Preference.() -> Unit
) = appendPreference(PreferenceCategory(context)) {
	setTitle(title)
	init()
}

@PreferenceDSL
fun PreferenceGroup.seekbarPreference(
	@StringRes title: Int,
	@StringRes description: Int? = null,
	min: Int = 0,
	max: Int = 100,
	increment: Int = 1,
	valueFormatter: DurationSeekBarPreference.ValueFormatter? = null,
	storeInit: PreferenceOptions.Builder<Int>.() -> Unit
) = appendPreference(DurationSeekBarPreference(context)) {
	val store = PreferenceOptions.Builder<Int>()
		.apply { storeInit() }
		.build()

	setTitle(title)

	if (description != null) setSummary(description)

	if (valueFormatter != null) this.valueFormatter = valueFormatter

	setMin(min)
	setMax(max)
	seekBarIncrement = increment
	showSeekBarValue = true

	value = store.get()
	isEnabled = store.enabled()
	isVisible = store.visible()
	setOnPreferenceChangeListener { _, newValue ->
		store.set(newValue as Int)
		value = store.get()

		// Always return false because we save it
		false
	}
}

@PreferenceDSL
fun PreferenceGroup.listPreference(
	@StringRes title: Int,
	entries: Map<String, String>,
	storeInit: PreferenceOptions.Builder<String>.() -> Unit
) = appendPreference(ListPreference(context)) {
	val store = PreferenceOptions.Builder<String>()
		.apply { storeInit() }
		.build()

	setTitle(title)

	entryValues = entries.keys.toTypedArray()
	setEntries(entries.values.toTypedArray())
	summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

	value = store.get()
	isEnabled = store.enabled()
	isVisible = store.visible()
	setOnPreferenceChangeListener { _, newValue ->
		store.set(newValue as String)
		value = store.get()

		// Always return false because we save it
		false
	}
}

@PreferenceDSL
fun PreferenceGroup.checkboxPreference(
	@StringRes title: Int,
	@StringRes description: Int? = null,
	storeInit: PreferenceOptions.Builder<Boolean>.() -> Unit
) = checkboxPreference(title, description, description, storeInit)

@PreferenceDSL
fun PreferenceGroup.checkboxPreference(
	@StringRes title: Int,
	@StringRes descriptionOn: Int? = null,
	@StringRes descriptionOff: Int? = null,
	storeInit: PreferenceOptions.Builder<Boolean>.() -> Unit
) = appendPreference(CheckBoxPreference(context)) {
	val store = PreferenceOptions.Builder<Boolean>()
		.apply { storeInit() }
		.build()

	setTitle(title)

	if (descriptionOn != null) setSummaryOn(descriptionOn)
	if (descriptionOff != null) setSummaryOff(descriptionOff)

	isChecked = store.get()
	isEnabled = store.enabled()
	isVisible = store.visible()
	setOnPreferenceChangeListener { _, newValue ->
		store.set(newValue as Boolean)
		isChecked = store.get()

		// Always return false because we save it
		false
	}
}

@PreferenceDSL
inline fun <reified T : Enum<T>> PreferenceGroup.enumPreference(
	@StringRes title: Int,
	storeInit: PreferenceOptions.Builder<T>.() -> Unit
) = appendPreference(ListPreference(context)) {
	val store = PreferenceOptions.Builder<T>()
		.apply { storeInit() }
		.build()

	// Calculate enum entries
	val values = enumValues<T>().mapNotNull { entry ->
		val options = T::class.java
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
	}.toMap()

	setTitle(title)

	entryValues = values.keys.toTypedArray()
	entries = values.values.toTypedArray()
	summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

	value = store.get().toString()
	isEnabled = store.enabled()
	isVisible = store.visible()
	setOnPreferenceChangeListener { _, newValue ->
		store.set(enumValues<T>().first { it.name == newValue })
		value = store.get().toString()

		// Always return false because we save it
		false
	}
}

@PreferenceDSL
fun PreferenceGroup.shortcutPreference(
	@StringRes title: Int,
	storeInit: PreferenceOptions.Builder<Int>.() -> Unit
) = appendPreference(ButtonRemapPreference(context, null)) {
	val store = PreferenceOptions.Builder<Int>()
		.apply { storeInit() }
		.build()

	setTitle(title)

	summaryProvider = ButtonRemapPreference.ButtonRemapSummaryProvider.instance

	setKeyCode(store.get())
	isEnabled = store.enabled()
	isVisible = store.visible()
	setOnPreferenceChangeListener { _, newValue ->
		store.set(newValue as Int)
		setKeyCode(store.get())

		// Always return false because we save it
		false
	}
}

@PreferenceDSL
fun PreferenceGroup.staticString(
	@StringRes title: Int,
	content: String
) = appendPreference(EditTextPreference(context)) {
	setTitle(title)
	isEnabled = false
	summary = content
}
