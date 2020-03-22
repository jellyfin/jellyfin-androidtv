package org.jellyfin.androidtv.details.actions

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.DelayedMessage
import org.jellyfin.apiclient.interaction.EmptyResponse

class DeleteAction(private val context: Context, private val item: LiveData<out BaseItem>, private val onItemDeleted: () -> Unit) : Action {
	override val visible = MediatorLiveData<Boolean>().apply {
		addSource(item) { value = it.deletable }
	}
	override val text = MutableLiveData(context.getString(R.string.lbl_delete))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_trash)!!)

	override suspend fun onClick(view: View) {
		val itemValue = item.value ?: return

		AlertDialog.Builder(context).apply {
			setIcon(R.drawable.ic_trash)
			setTitle(R.string.lbl_really_delete_item_title)
			setMessage(context.getString(R.string.lbl_item_deletion_warning, itemValue.title))
			setPositiveButton(R.string.lbl_delete) { _, _ ->
				val msg = DelayedMessage(context, 150)
				//todo coroutine version for api call
				TvApp.getApplication().apiClient.DeleteItem(itemValue.id, object : EmptyResponse() {
					override fun onResponse() {
						msg.Cancel()
						Toast.makeText(context, context.getString(R.string.lbl_item_deleted, itemValue.title), Toast.LENGTH_LONG).show()
						TvApp.getApplication().lastDeletedItemId = itemValue.id
						onItemDeleted()
					}

					override fun onError(ex: Exception) {
						msg.Cancel()
						TvApp.getApplication().logger.ErrorException("Failed to delete item %s", ex, itemValue.title)
						Toast.makeText(context, ex.localizedMessage, Toast.LENGTH_LONG).show()
					}
				})
			}
			setNegativeButton(context.getText(R.string.lbl_cancel)) { _, _ -> Toast.makeText(context, R.string.lbl_item_not_deleted, Toast.LENGTH_LONG).show() }
			show().getButton(BUTTON_NEGATIVE).requestFocus()
		}
	}

}
