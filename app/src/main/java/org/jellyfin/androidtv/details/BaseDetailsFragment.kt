package org.jellyfin.androidtv.details

import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BaseDetailsFragment : DetailsSupportFragment() {
	protected lateinit var rowsAdapter: ArrayObjectAdapter


	protected suspend fun getImageFromURL(url: String) = withContext(Dispatchers.IO) {
		Picasso.with(activity).load(url).get()
	}
}
