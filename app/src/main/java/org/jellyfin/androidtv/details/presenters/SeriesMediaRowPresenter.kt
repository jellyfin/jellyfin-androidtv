package org.jellyfin.androidtv.details.presenters

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.leanback.widget.RowPresenter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_details_series_media.view.*
import kotlinx.android.synthetic.main.row_details_series_media_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.details.rows.SeriesMediaRow
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.model.itemtypes.Season
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.getEpisodesOfSeason
import org.jellyfin.androidtv.util.dp

class SeriesMediaRowPresenter(
	private val context: Context
) : RowPresenter() {
	init {
		headerPresenter = null
		selectEffectEnabled = false
	}

	class ViewHolder(view: View) : RowPresenter.ViewHolder(view) {
		val seasonList = view.season_list.apply {
			adapter = SeasonListAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(16.dp))
		}
		val poster = view.poster
		val seasonContainer = view.season_container.apply {
			adapter = SeasonContainerAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(16.dp))
		}
	}

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_details_series_media, parent, false)

		return ViewHolder(view)
	}

	override fun onBindRowViewHolder(viewHolder: RowPresenter.ViewHolder, row: Any) {
		viewHolder as ViewHolder
		row as SeriesMediaRow
		val series = row.item

		(viewHolder.seasonList.adapter as SeasonListAdapter).apply {
			setItems(row.seasons)
			setItemChangeListener {
				setSelectedSeason(viewHolder, it)
			}
		}
	}

	private fun setSelectedSeason(vh: ViewHolder, item: Season) {
		item.images.primary?.load(context) {
			vh.poster.setImageBitmap(it)
		}

		Toast.makeText(context, item.title, Toast.LENGTH_LONG).show()
		GlobalScope.launch(Dispatchers.IO) {
			val episodes = TvApp.getApplication().apiClient.getEpisodesOfSeason(item) ?: emptyList()
			withContext(Dispatchers.Main) {
				(vh.seasonContainer.adapter as SeasonContainerAdapter).apply {
					setItems(episodes)
				}
			}
		}

	}

	class SeasonListAdapter : RecyclerView.Adapter<SeasonListAdapter.ViewHolder>() {
		private var onChange: ((item: Season) -> Unit)? = null
		private var items = emptyList<Season>()

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val button = view as Button
		}

		fun setItems(items: List<Season>) {
			this.items = items
			notifyDataSetChanged()
		}

		fun setItemChangeListener(onChange: (item: Season) -> Unit) {
			this.onChange = onChange
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater
				.from(parent.context)
				.inflate(R.layout.row_details_series_media_button, parent, false)

			return ViewHolder(view)
		}

		override fun getItemCount(): Int = items.size

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]

			holder.button.text = item.title
			holder.button.setOnFocusChangeListener { _, hasFocus ->
				if (hasFocus) onChange?.invoke(item)
			}
		}
	}

	class SeasonContainerAdapter : RecyclerView.Adapter<SeasonContainerAdapter.ViewHolder>() {
		private var items = emptyList<Episode>()

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val thumbnail: ImageView = view.thumbnail
			val title: TextView = view.title
			val duration: TextView = view.duration
			val endsAt: TextView = view.ends_at
		}

		fun setItems(items: List<Episode>) {
			this.items = items
			notifyDataSetChanged()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater
				.from(parent.context)
				.inflate(R.layout.row_details_series_media_item, parent, false)

			return ViewHolder(view)
		}

		override fun getItemCount(): Int = items.size

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val item = items[position]

			item.images.primary?.let { image ->
				GlobalScope.launch(Dispatchers.Main) {
					holder.thumbnail.setImageBitmap(image.getBitmap(holder.thumbnail.context))
				}
			}
			holder.title.text = item.title

			if (item.durationTicks != null) {
				holder.duration.text = TimeUtils.formatMillis(item.durationTicks / 10000)
				holder.endsAt.text = DateFormat.getTimeFormat(holder.endsAt.context).format(System.currentTimeMillis() + (item.durationTicks - item.playbackPositionTicks) / 10000)
			}
		}
	}
}
