package org.jellyfin.androidtv.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.badge_watched.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.PlayableItem


class WatchedBadge(context: Context, val item: PlayableItem? = null) : FrameLayout(context) {
	private val changeListener: () -> Unit = {
		item?.let(::setItemData)
	}

	init {
		val inflater = LayoutInflater.from(context)
		inflater.inflate(R.layout.badge_watched, this)
		item?.let(::setItemData)
	}

	constructor(context: Context) : this(context, null)

	fun setWatched() {
		badge_background.visibility = View.VISIBLE
		badge_watched_tick.visibility = View.VISIBLE
		badge_counter.visibility = View.GONE
	}

	fun setRemainingCount(count: Int) {
		badge_background.visibility = View.VISIBLE
		badge_watched_tick.visibility = View.GONE
		badge_counter.visibility = View.VISIBLE
		badge_counter.text = count.toString()
	}

	fun clear() {
		badge_background.visibility = View.GONE
		badge_watched_tick.visibility = View.GONE
		badge_counter.visibility = View.GONE
	}

	private fun setItemData(item: PlayableItem) {
		if (item.played) setWatched() else clear()
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		item?.addChangeListener(changeListener)
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		item?.removeChangeListener(changeListener)
	}
}
