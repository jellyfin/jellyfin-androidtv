package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import android.view.View
import androidx.leanback.app.RowsSupportFragment

open class AutoFocusRowsFragment : RowsSupportFragment() {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		verticalGridView.requestFocus()
	}
}
