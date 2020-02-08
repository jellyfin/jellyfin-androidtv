package org.jellyfin.androidtv.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R
import org.jellyfin.apiclient.model.dto.GenreDto

class GenreAdapter : RecyclerView.Adapter<GenreAdapter.ViewHolder>() {
	private var items = emptyList<GenreDto>()

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val button = view as Button
	}

	fun setItems(items: List<GenreDto>) {
		this.items = items
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_details_description_genre_button, parent, false)

		return ViewHolder(view)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val genre = items[position]

		holder.button.text = genre.name

		//todo open browse for library of item filtered by genre
		holder.button.setOnClickListener { Toast.makeText(holder.button.context, "Test: ${genre.name} / ${genre.id}", Toast.LENGTH_LONG).show() }
	}
}
