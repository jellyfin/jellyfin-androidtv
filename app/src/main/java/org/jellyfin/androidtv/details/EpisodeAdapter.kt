package org.jellyfin.androidtv.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.databinding.EpisodeItemBinding
import org.jellyfin.androidtv.model.itemtypes.Episode

class EpisodeAdapter(private val onFocusChange: (episode: Episode, view: View, focus: Boolean) -> Unit) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {
	private var items: List<Episode> = listOf()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		val binding = EpisodeItemBinding.inflate(inflater, parent, false)

		return ViewHolder(binding)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val episode = items[position]
		holder.title.text = episode.title
		holder.description.text = episode.description

		holder.itemView.setOnFocusChangeListener { view, focus ->
			onFocusChange(episode, view, focus)
		}
	}

	fun setItems(items: List<Episode>) {
		this.items = items
		notifyDataSetChanged()
	}

	class ViewHolder(binding: EpisodeItemBinding) : RecyclerView.ViewHolder(binding.root) {
		val title by lazy { binding.title }
		val description by lazy { binding.description }
	}
}
