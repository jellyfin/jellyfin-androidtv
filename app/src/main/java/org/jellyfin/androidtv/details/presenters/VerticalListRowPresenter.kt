package org.jellyfin.androidtv.details.presenters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.RowPresenter
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.row_vertical_list.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.rows.VerticalListRow

class VerticalListRowPresenter : RowPresenter() {
	init {
		headerPresenter = null
		selectEffectEnabled = false
	}

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_vertical_list, parent, false)

		return ViewHolder(view)
	}

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)

		val vh = holder as ViewHolder
		val rowItem = item as VerticalListRow

		vh.itemBridgeAdapter.setAdapter(rowItem.adapter)
		vh.items.apply {
			layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
			adapter = vh.itemBridgeAdapter
		}
	}

	class ViewHolder(view: View) : RowPresenter.ViewHolder(view) {
		val itemBridgeAdapter = ItemBridgeAdapter()

		val items = view.vertical_list_items!!
	}
}
