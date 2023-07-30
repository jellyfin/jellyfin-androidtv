package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
	private var rowsAdapter: MutableObjectAdapter<Row>? = null
	private var rowAdded = false

	init {
		notificationsRepository.notifications.onEach { notifications ->
			announcementAdapter.replaceAll(notifications)
			update(notifications.isEmpty())
		}.launchIn(lifecycleScope)
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

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		this.rowsAdapter = rowsAdapter
		update(notificationsRepository.notifications.value.isEmpty())
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (item !is AppNotification) return

		notificationsRepository.dismissNotification(item)
	}
}
