package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.ui.card.MediaInfoCardView
import org.jellyfin.sdk.model.api.MediaStream

class InfoCardPresenter : Presenter() {
	class ViewHolder(
		private val mediaInfoCardView: MediaInfoCardView
	) : Presenter.ViewHolder(mediaInfoCardView) {
		fun setItem(mediaStream: MediaStream) = mediaInfoCardView.setMediaStream(mediaStream)
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = MediaInfoCardView(parent.context).apply {
			isFocusable = true
			isFocusableInTouchMode = true
		}

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		if (item !is MediaStream) return
		if (viewHolder !is ViewHolder) return

		viewHolder.setItem(item)
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
}
