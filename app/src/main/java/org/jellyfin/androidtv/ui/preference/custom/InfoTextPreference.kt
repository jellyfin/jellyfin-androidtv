package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.jellyfin.androidtv.R

class InfoTextPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

	init {
		layoutResource = R.layout.preference_info_text
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)

		val titleView = holder.findViewById(R.id.textInfo) as? TextView

		titleView?.text = title

	}
}
