package org.jellyfin.androidtv.details.actions

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.apiclient.updateFavoriteStatus

class ToggleFavoriteAction(context: Context, val item: BaseItem) : ToggleAction(ActionID.TOGGLE_FAVORITE.id, context) {
	init {
		active = item.favorite
		label1 = context.getString(R.string.lbl_favorite)
	}

	override fun onClick() {
		GlobalScope.launch(Dispatchers.Main) {
			val apiClient = TvApp.getApplication().apiClient

			apiClient.updateFavoriteStatus(
				item.id,
				TvApp.getApplication().currentUser.id,
				!item.favorite
			)?.let {
				item.favorite = it.isFavorite
			}
		}
	}
}
