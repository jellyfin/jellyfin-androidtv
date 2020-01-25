package org.jellyfin.androidtv.details

import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter

open class BaseDetailsFragment : DetailsSupportFragment() {
	protected lateinit var rowsAdapter: ArrayObjectAdapter
}
