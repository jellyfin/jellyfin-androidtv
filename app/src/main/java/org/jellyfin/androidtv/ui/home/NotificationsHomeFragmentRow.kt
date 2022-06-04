package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.model.AppNotification
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.ui.notification.AppNotificationPresenter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

class NotificationsHomeFragmentRow(
	lifecycleScope: LifecycleCoroutineScope,
	private val notificationsRepository: NotificationsRepository,
) : HomeFragmentRow, OnItemViewClickedListener {
	private val announcementAdapter by lazy { MutableObjectAdapter<AppNotification>(AppNotificationPresenter()) }
	private val listRow by lazy { ListRow(null, announcementAdapter) }
	private var rowsAdapter: ArrayObjectAdapter? = null
	private var rowAdded = false

	init {
		lifecycleScope.launch {
			notificationsRepository.notifications.collect { notifications ->
				announcementAdapter.replaceAll(notifications)
				update(notifications.isEmpty())
			}
		}
	}

	private fun update(empty: Boolean) {
		if (rowsAdapter == null) return

		if (empty && rowAdded) {
			rowsAdapter?.remove(listRow)
			rowAdded = false
		}

		if (!empty && !rowAdded) {
			rowsAdapter?.add(0, listRow)
			rowAdded = true
		}
	}

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		this.rowsAdapter = rowsAdapter
		update(notificationsRepository.notifications.value.isEmpty())
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (item !is AppNotification) return

		notificationsRepository.dismissNotification(item)
	}
}
