package org.jellyfin.androidtv.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem

class HomeViewModel : ViewModel() {
	private val _focusedItem = MutableStateFlow<BaseRowItem?>(null)
	val focusedItem = _focusedItem.asStateFlow()

	fun updateFocusedItem(item: BaseRowItem?) {
		_focusedItem.value = item
	}
}
