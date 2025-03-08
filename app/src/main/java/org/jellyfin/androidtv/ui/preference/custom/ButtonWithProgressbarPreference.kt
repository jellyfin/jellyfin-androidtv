package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.jellyfin.androidtv.R

class ButtonWithProgressbarPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var progressBar: ProgressBar? = null
    private var btnLogin: Button? = null

	init {
		layoutResource = R.layout.preference_button_with_progressbar
	}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        progressBar = holder.findViewById(R.id.loading_bar) as ProgressBar
        btnLogin = holder.findViewById(R.id.btn_login) as Button
		btnLogin?.text = title

		progressBar?.visibility = View.GONE
		btnLogin?.setOnClickListener {
			onPreferenceClickListener?.onPreferenceClick(this)
		}
    }

    fun setLoading(isLoading: Boolean) {
		if(isLoading){
			progressBar?.visibility = View.VISIBLE
			btnLogin?.visibility = View.GONE
		}else{
			progressBar?.visibility = View.GONE
			btnLogin?.visibility = View.VISIBLE
		}
    }

	override fun setOnPreferenceClickListener(onPreferenceClickListener: OnPreferenceClickListener?) {
		super.setOnPreferenceClickListener(onPreferenceClickListener)

		btnLogin?.setOnClickListener {
			onPreferenceClickListener?.onPreferenceClick(this)
		}

	}
}
