package org.jellyfin.androidtv.details.presenters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import kotlinx.android.synthetic.main.list_item_song.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Audio
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.getItem

class SongPresenter : Presenter() {
	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.list_item_song, parent, false)

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
		val audio = item as Audio
		val vh = viewHolder as ViewHolder

		vh.container.setOnClickListener { showMenu(it, audio) }
		vh.index.text = audio.index.toString()
		vh.title.text = audio.title
		vh.artist.text = audio.artists.joinToString(", ")
		vh.duration.text = audio.durationTicks?.let { TimeUtils.formatMillis(it / 10000L) }
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
		val vh = viewHolder as ViewHolder

		vh.container.setOnClickListener(null)
	}

	private fun showMenu(row: View, item: Audio) {
		Utils.createPopupMenu(row.context, row, Gravity.END).apply {
			menu.add(R.string.lbl_play).setOnMenuItemClickListener {
				GlobalScope.launch(Dispatchers.IO) {
					val baseItem = TvApp.getApplication().apiClient.getItem(item.id)!!

					withContext(Dispatchers.Main) { MediaManager.playNow(baseItem) }
				}

				true
			}

			menu.add(R.string.lbl_add_to_queue).setOnMenuItemClickListener {
				GlobalScope.launch(Dispatchers.IO) {
					val baseItem = TvApp.getApplication().apiClient.getItem(item.id)!!

					withContext(Dispatchers.Main) { MediaManager.queueAudioItem(baseItem) }
				}

				true
			}

			menu.add(R.string.lbl_instant_mix).setOnMenuItemClickListener {
				PlaybackHelper.playInstantMix(item.id)

				true
			}
		}.show()
	}

	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val container = view.list_item_song!!
		val index = view.list_item_song_index!!
		val title = view.list_item_song_title!!
		val artist = view.list_item_song_artist!!
		val duration = view.list_item_song_duration!!
	}
}
