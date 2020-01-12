package org.jellyfin.androidtv.playback.nextup

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_upnext_row.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.toHtmlSpanned

class UpNextDetailsPresenter(val context: Context) : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		return ViewHolder(LayoutInflater.from(context).inflate(R.layout.fragment_upnext_row, parent, false))
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
		item as UpNextItemData

		Picasso.with(context).load(item.thumbnail).into(viewHolder.view.image)
		viewHolder.view.title.text = item.title
		viewHolder.view.description.text = item.description?.toHtmlSpanned()
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		// Nothing to do here.
	}
}
