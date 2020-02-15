package org.jellyfin.androidtv.details

import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.bold
import androidx.leanback.widget.Presenter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_details_description.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
import org.jellyfin.androidtv.ui.widget.Rating
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.apiclient.model.entities.MediaStreamType

class DetailsDescriptionPresenter : Presenter() {
	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val title: TextView = view.findViewById(R.id.details_description_title)
		val subtitle: TextView = view.findViewById(R.id.details_description_subtitle)

		val year: TextView = view.details_description_year
		val officialRating: TextView = view.details_description_official_rating
		val communityRating: Rating = view.details_description_community_rating
		val criticsRating: Rating = view.details_description_critics_rating

		val genres: RecyclerView = view.details_description_genres.apply {
			adapter = GenreAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(8))
		}

		val body: TextView = view.details_description_body

		val tags: TextView = view.details_description_tags

		val durationInfo: LinearLayout = view.details_description_duration_info
		val duration: TextView = view.details_description_duration_info_duration
		val endsAt: TextView = view.details_description_duration_info_end

		val streams: LinearLayout = view.details_description_streams
		val videoStreamLabel: TextView = view.details_description_streams_video_label
		val videoStreamValue: TextView = view.details_description_streams_video_value
		val audioStreamLabel: TextView = view.details_description_streams_audio_label
		val audioStreamValue: TextView = view.details_description_streams_audio_value
		val textStreamLabel: TextView = view.details_description_streams_text_label
		val textStreamValue: TextView = view.details_description_streams_text_value
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_details_description, parent, false)

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
		viewHolder as ViewHolder
		item as BaseItem

		// title
		viewHolder.title.text = item.title

		// Original title
		if (item.titleOriginal != null && item.titleOriginal != item.title) {
			viewHolder.subtitle.text = item.titleOriginal
			viewHolder.subtitle.visibility = View.VISIBLE
		} else {
			viewHolder.subtitle.visibility = View.GONE
		}

		// rating
		if (item is Movie) { //todo move those properties to baseitem or something
			viewHolder.year.text = item.productionYear.toString()

			if (item.officialRating != null) {
				viewHolder.officialRating.text = item.officialRating
				viewHolder.officialRating.visibility = View.VISIBLE
			} else {
				viewHolder.officialRating.visibility = View.GONE
			}

			viewHolder.communityRating.value = item.communityRating

			if (item.criticsRating != null) {
				viewHolder.criticsRating.value = item.criticsRating
				viewHolder.criticsRating.visibility = View.VISIBLE
			} else {
				viewHolder.criticsRating.visibility = View.GONE
			}
		}

		if (item is PlayableItem) {
			(viewHolder.genres.adapter as GenreAdapter).setItems(item.genres)

			val videoStream = item.mediaInfo.streams.find { it.type == MediaStreamType.Video }
			if (videoStream != null) {
				viewHolder.videoStreamValue.text = videoStream.displayTitle
				viewHolder.videoStreamLabel.visibility = View.VISIBLE
				viewHolder.videoStreamValue.visibility = View.VISIBLE
			} else {
				viewHolder.videoStreamLabel.visibility = View.GONE
				viewHolder.videoStreamValue.visibility = View.GONE
			}

			val audioStream = item.mediaInfo.streams.find { it.type == MediaStreamType.Audio }
			if (audioStream != null) {
				viewHolder.audioStreamValue.text = audioStream.displayTitle
				viewHolder.audioStreamLabel.visibility = View.VISIBLE
				viewHolder.audioStreamValue.visibility = View.VISIBLE
			} else {
				viewHolder.audioStreamLabel.visibility = View.GONE
				viewHolder.audioStreamValue.visibility = View.GONE
			}

			val textStream = item.mediaInfo.streams.find { it.type == MediaStreamType.Subtitle }
			if (textStream != null) {
				viewHolder.textStreamValue.text = textStream.displayTitle
				viewHolder.textStreamLabel.visibility = View.VISIBLE
				viewHolder.textStreamValue.visibility = View.VISIBLE
			} else {
				viewHolder.textStreamLabel.visibility = View.GONE
				viewHolder.textStreamValue.visibility = View.GONE
			}

			viewHolder.streams.visibility = View.VISIBLE

			viewHolder.duration.text = TimeUtils.formatMillis(item.durationTicks / 10000)
			viewHolder.endsAt.text = DateFormat.getTimeFormat(viewHolder.view.context).format(System.currentTimeMillis() + (item.durationTicks - item.playbackPositionTicks) / 10000)
			viewHolder.durationInfo.visibility = View.VISIBLE
		} else {
			viewHolder.streams.visibility = View.GONE
			viewHolder.durationInfo.visibility = View.GONE
		}

		if (item is PlayableItem && item.tags.isNotEmpty()) {
			viewHolder.tags.text = SpannableStringBuilder()
				.bold { append("Tags: ") }
				.append(item.tags.joinToString(", "))
			viewHolder.tags.visibility = View.VISIBLE
		} else {
			viewHolder.tags.visibility = View.GONE
		}

		// description
		viewHolder.body.text = item.description
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {}
}
