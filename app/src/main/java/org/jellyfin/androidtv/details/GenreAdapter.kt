package org.jellyfin.androidtv.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R

class GenreAdapter : RecyclerView.Adapter<GenreAdapter.ViewHolder>() {
	private var items = emptyList<String>()

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val button = view as Button
	}

	fun setItems(items: List<String>) {
		this.items = items
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.row_details_description_genre_button, parent, false)

		return ViewHolder(view)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.button.text = items[position]
	}
}
