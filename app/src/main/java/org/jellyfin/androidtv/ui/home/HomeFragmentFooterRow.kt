package org.jellyfin.androidtv.ui.home

import android.app.Activity
import android.content.Intent
import androidx.leanback.widget.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.startup.StartupActivity

class HomeFragmentFooterRow(
	private val activity: Activity,
	private val sessionRepository: SessionRepository,
) : HomeFragmentRow, OnItemViewClickedListener {
	override fun addToRowsAdapter(cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		val header = HeaderItem(rowsAdapter.size().toLong(), activity.getString(R.string.lbl_settings))
		val adapter = ArrayObjectAdapter(GridButtonPresenter()).apply {
			add(GridButton(BUTTON_SETTINGS, activity.getString(R.string.lbl_settings), R.drawable.tile_settings))
			add(GridButton(BUTTON_SWITCH_USER, activity.getString(R.string.lbl_switch_user), R.drawable.tile_switch_user))
		}

		rowsAdapter.add(ListRow(header, adapter))
	}

	override fun onItemClicked(
		itemViewHolder: Presenter.ViewHolder,
		item: Any,
		rowViewHolder: RowPresenter.ViewHolder,
		row: Row
	) {
		if (item !is GridButton) return

		when (item.id) {
			BUTTON_SWITCH_USER -> {
				sessionRepository.destroyCurrentSession()

				// Open login activity
				val selectUserIntent = Intent(activity, StartupActivity::class.java)
				selectUserIntent.putExtra(StartupActivity.HIDE_SPLASH, true)
				// Remove history to prevent user going back to current activity
				selectUserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

				activity.startActivity(selectUserIntent)
				activity.finish()
			}

			BUTTON_SETTINGS -> {
				val settingsIntent = Intent(activity, PreferencesActivity::class.java)
				activity.startActivity(settingsIntent)
			}
		}
	}

	companion object {
		private const val BUTTON_SWITCH_USER = 0
		private const val BUTTON_SETTINGS = 1
	}
}
