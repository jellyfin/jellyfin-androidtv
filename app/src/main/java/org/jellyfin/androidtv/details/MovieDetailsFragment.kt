package org.jellyfin.androidtv.details

import android.content.Intent
import android.util.Log
import androidx.leanback.widget.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.actions.*
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.Person
import org.jellyfin.androidtv.presentation.InfoCardPresenter

private const val LOG_TAG = "MovieDetailsFragment"

class MovieDetailsFragment(item: Movie) : BaseDetailsFragment<Movie>(item), OnItemViewClickedListener {

	private val detailsRow by lazy { DetailsOverviewRow(Unit).apply { actionsAdapter = ActionAdapter() } }
//	private val chaptersRow by lazy { Row() }
//	private val staffRow by lazy { Row() }
	private val charactersRow by lazy { ListRow(HeaderItem("Cast/Crew"), ArrayObjectAdapter(PersonPresenter())) }
//	private val relatedRow by lazy { Row() }
	private val mediaInfoRow by lazy { ListRow(HeaderItem("Media info"), ArrayObjectAdapter(InfoCardPresenter())) }

	override fun onCreateAdapter(adapter: ArrayObjectAdapter, selector: ClassPresenterSelector) {
		super.onCreateAdapter(adapter, selector)

		// Add presenters
		selector.addClassPresenter(ListRow::class.java, ListRowPresenter())

		// Add rows
		adapter.add(detailsRow)
//		adapter.add(chaptersRow)
//		adapter.add(staffRow)
		adapter.add(charactersRow)
//		adapter.add(relatedRow)
		adapter.add(mediaInfoRow)

		onItemViewClickedListener = this
	}

	override suspend fun setItem(item: Movie) {
		super.setItem(item)

		// Update detail row
		detailsRow.item = item

		// Update actions
		(detailsRow.actionsAdapter as ActionAdapter).apply {
			reset()

			if (item.canResume) add(ResumeAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_resume) })

			add(PlayFromBeginningAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_play) })
			add(ToggleWatchedAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_watch) })
			add(ToggleFavoriteAction(context!!, item).apply { icon = context!!.getDrawable(R.drawable.ic_heart) })
			add(Action(0, "More").apply { icon = context!!.getDrawable(R.drawable.ic_more) }) // Show menu with more options

			commit()
		}

		detailsRow.setImageBitmap(context!!, item.images.primary?.getBitmap(context!!))


		charactersRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.cast.forEach(it::add)
		}


		// Update media info data
		mediaInfoRow.adapter.also {
			it as ArrayObjectAdapter
			it.clear()
			item.mediaInfo.streams.forEach(it::add)
		}
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (row == charactersRow) {
			val person = item as Person
			val intent = Intent(activity, FullDetailsActivity::class.java)
			intent.putExtra("ItemId", person.id)

			activity!!.startActivity(intent)
		}
	}
}
