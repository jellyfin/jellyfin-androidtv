package org.jellyfin.androidtv.details

import android.os.Bundle
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*

open class BaseDetailsFragment : DetailsSupportFragment() {
	protected lateinit var rowsAdapter: ArrayObjectAdapter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
	}

}
