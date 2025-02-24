package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.graphics.Color
import androidx.preference.ListPreference
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.custom.ColorPickerDialogFragment.ColorListItem

class ColorListPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
	defStyleRes: Int = 0,
) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {
	var items: List<ColorListItem> = emptyList()

	fun getItem(key: String) = items.firstOrNull { it.key == Color(key.toInt()) }

	fun getCurrentItem() = value?.let(::getItem)

	class SimpleSummaryProvider : SummaryProvider<ColorListPreference> {
		override fun provideSummary(preference: ColorListPreference) =
			preference.getCurrentItem()?.title ?: preference.context.getString(R.string.not_set)

		companion object {
			val instance by lazy { SimpleSummaryProvider() }
		}
	}
}

