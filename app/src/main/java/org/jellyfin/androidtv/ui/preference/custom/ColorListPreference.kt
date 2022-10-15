package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.custom.ColorPickerDialogFragment.ColorListItem
import org.jellyfin.androidtv.ui.preference.custom.ColorPickerDialogFragment.ColorListItem.ColorListOption

class ColorListPreference<String> @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
	defStyleRes: Int = 0,
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
	private var items: List<ColorListItem<String>> = emptyList()
	var value: String? = null

	fun setItems(items: Map<Long, String>) {
		this.items = items.entries.map {
			ColorListOption(it.key, it.value.toString())
		}
	}

	fun getItems(): List<ColorListItem<String>> = items
	fun getOptions(): List<ColorListOption<String>> = getItems().filterIsInstance<ColorListOption<String>>()
	fun getItem(key: String) = getOptions().firstOrNull { it.key.toString() == key }
	fun getCurrentItem() = value?.let(::getItem)

	class SimpleSummaryProvider : SummaryProvider<ColorListPreference<*>> {
		override fun provideSummary(preference: ColorListPreference<*>) =
			preference.getCurrentItem()?.title ?: preference.context.getString(R.string.not_set)

		companion object {
			val instance by lazy { SimpleSummaryProvider() }
		}
	}
}

