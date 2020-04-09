package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.view.View
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.DetailsActivity
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class GoToItemAction(private val context: Context, label: String, private val target: BaseItem) : Action {
	override val visible = MutableLiveData(true)
	override val text = MutableLiveData(label)
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_folder)!!)

	override suspend fun onClick(view: View?) {
		DetailsActivity.start(context, target)
	}
}
