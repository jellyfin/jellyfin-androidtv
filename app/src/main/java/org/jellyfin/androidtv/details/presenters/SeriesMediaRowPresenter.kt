package org.jellyfin.androidtv.details.presenters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.leanback.widget.RowPresenter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_details_series_media.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.rows.SeriesMediaRow
import org.jellyfin.androidtv.model.itemtypes.Season
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
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
		val seasonContainer = view.season_container
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
}
