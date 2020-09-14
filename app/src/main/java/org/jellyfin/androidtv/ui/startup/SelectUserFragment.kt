package org.jellyfin.androidtv.ui.startup

import android.content.Intent
import android.widget.Toast
import androidx.leanback.widget.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.browsing.CustomBrowseFragment
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import org.jellyfin.apiclient.interaction.EmptyResponse

class SelectUserFragment : CustomBrowseFragment() {
	override fun addAdditionalRows(rowAdapter: ArrayObjectAdapter) {
		super.addAdditionalRows(rowAdapter)
		val manualEntryRowItem = BaseRowItem(
			GridButton(ENTER_MANUALLY, getString(R.string.lbl_enter_manually), R.drawable.tile_edit)
		)

		val usersHeader = HeaderItem(rowAdapter.size().toLong(), getString(R.string.lbl_select_user))
		val usersAdapter = ItemRowAdapter(CardPresenter(), rowAdapter)

		usersAdapter.add(manualEntryRowItem)

		usersAdapter.setRetrieveFinishedListener(
			UserRetrieveFinishedListener(usersAdapter, manualEntryRowItem)
		)

		usersAdapter.Retrieve()

		rowAdapter.add(ListRow(usersHeader, usersAdapter))
		val gridHeader = HeaderItem(rowAdapter.size().toLong(), getString(R.string.lbl_other_options))
		val mGridPresenter = GridButtonPresenter()
		val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)

		gridRowAdapter.add(
			GridButton(
				SWITCH_SERVER,
				getString(R.string.lbl_switch_server),
				R.drawable.tile_port_server
			)
		)

		rowAdapter.add(ListRow(gridHeader, gridRowAdapter))
	}

	override fun setupEventListeners() {
		super.setupEventListeners()
		mClickedListener.registerListener(ItemViewClickedListener())
	}

	private inner class ItemViewClickedListener : OnItemViewClickedListener {
		override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
								   rowViewHolder: RowPresenter.ViewHolder, row: Row) {
			if (item is GridButton) {
				when (item.id) {
					SWITCH_SERVER -> {
						// Present server selection
						val serverIntent = Intent(activity, SelectServerActivity::class.java)
						serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
						requireActivity().startActivity(serverIntent)
					}
					// Manual login
					ENTER_MANUALLY ->  AuthenticationHelper.enterManualUser(activity)
					else -> Toast.makeText(activity, item.toString(), Toast.LENGTH_SHORT).show()
				}
			} else if (item is BaseRowItem) {
				if (item.gridButton != null &&
						item.gridButton.id == ENTER_MANUALLY) {
					// Manual login
					AuthenticationHelper.enterManualUser(activity)
				}
			}
		}
	}

	private class UserRetrieveFinishedListener(
		private val usersAdapter: ItemRowAdapter,
		private val rowItem: BaseRowItem
	) : EmptyResponse() {
		override fun onResponse() {
			usersAdapter.remove(rowItem)
			usersAdapter.add(rowItem)
		}
	}

	companion object {
		private const val ENTER_MANUALLY = 0
		private const val SWITCH_SERVER = 3
	}
}
