package org.jellyfin.androidtv.details.actions

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface.BUTTON_NEGATIVE
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.DelayedMessage
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.apiclient.interaction.EmptyResponse

class DeleteAction(context: Context, private val item: BaseItem, private val onItemDeleted: () -> Unit) : BaseAction(ActionID.DELETE.id, context) {

	init {
		label1 = context.getString(R.string.lbl_delete)
		icon = context.getDrawable(R.drawable.ic_trash)
	}

	override fun onClick() {
		AlertDialog.Builder(context).apply {
			setIcon(R.drawable.ic_trash)
			setTitle(R.string.lbl_really_delete_item_title)
			setMessage(context.getString(R.string.lbl_item_deletion_warning, item.title))
			setPositiveButton(R.string.lbl_delete) { _, _ ->
				val msg = DelayedMessage(context, 150)
				TvApp.getApplication().apiClient.DeleteItem(item.id, object : EmptyResponse() {
					override fun onResponse() {
						msg.Cancel()
						Utils.showToast(context, context.getString(R.string.lbl_item_deleted, item.title))
						TvApp.getApplication().lastDeletedItemId = item.id
						onItemDeleted()
					}

					override fun onError(ex: Exception) {
						msg.Cancel()
						TvApp.getApplication().logger.ErrorException("Failed to delete item %s", ex, item.title)
						Utils.showToast(context, ex.localizedMessage)
					}
				})
			}
			setNegativeButton(context.getText(R.string.lbl_cancel)) { dialog, which -> Utils.showToast(context, R.string.lbl_item_not_deleted) }
			show().getButton(BUTTON_NEGATIVE).requestFocus()
		}
	}

}
