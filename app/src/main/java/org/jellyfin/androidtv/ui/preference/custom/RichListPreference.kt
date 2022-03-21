package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListOption
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListSection

class RichListPreference<K> @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
	defStyleRes: Int = 0,
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
	private var items: List<RichListItem<K>> = emptyList()
	var value: K? = null

	fun setItems(items: List<RichListItem<K>>) {
		this.items = items
	}

	@JvmName("setItemsByGroup")
	fun setItems(items: Map<String, List<RichListItem<K>>>) {
		this.items = buildList {
			items.forEach { item ->
				add(RichListSection<K>(item.key))
				item.value.forEach(::add)
			}
		}
	}

	fun setItems(items: Map<K, String>) {
		this.items = items.entries.map {
			RichListOption(it.key, it.value)
		}
	}

	fun getItems(): List<RichListItem<K>> = items
	fun getOptions(): List<RichListOption<K>> = getItems().filterIsInstance<RichListOption<K>>()
	fun getItem(key: K) = getOptions().firstOrNull { it.key == key }
	fun getCurrentItem() = value?.let(::getItem)

	class SimpleSummaryProvider : SummaryProvider<RichListPreference<*>> {
		override fun provideSummary(preference: RichListPreference<*>) =
			preference.getCurrentItem()?.title ?: preference.context.getString(R.string.not_set)

		companion object {
			val instance by lazy { SimpleSummaryProvider() }
		}
	}
}

