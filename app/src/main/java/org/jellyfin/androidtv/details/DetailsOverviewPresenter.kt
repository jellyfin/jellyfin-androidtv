package org.jellyfin.androidtv.details

import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.bold
import androidx.leanback.widget.RowPresenter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.row_details_description.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.actions.ActionAdapter
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
import org.jellyfin.androidtv.ui.widget.Rating
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.dp
import org.jellyfin.apiclient.model.entities.MediaStreamType

class DetailsOverviewPresenter : RowPresenter() {
	private val actionAdapter = ActionAdapter()

	init {
		headerPresenter = null
		selectEffectEnabled = false
	}

	class ViewHolder(view: View) : RowPresenter.ViewHolder(view) {
		val banner: ImageView = view.details_description_banner
		val logo: ImageView = view.details_description_logo
		val poster: ImageView = view.details_description_poster

		val actions: LinearLayout = view.details_description_actions
		val actionViewHolders = mutableListOf<ActionAdapter.ActionViewHolder>()

		val title: TextView = view.findViewById(R.id.details_description_title)
		val subtitle: TextView = view.findViewById(R.id.details_description_subtitle)

		val year: TextView = view.details_description_year
		val officialRating: TextView = view.details_description_official_rating
		val communityRating: Rating = view.details_description_community_rating
		val criticsRating: Rating = view.details_description_critics_rating

		val genres: RecyclerView = view.details_description_genres.apply {
			adapter = GenreAdapter()
			addItemDecoration(RecyclerViewSpacingDecoration(4.dp))
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

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater
			.from(parent.context)
			.inflate(R.layout.row_details_description, parent, false)

		return ViewHolder(view)
	}

	override fun onBindRowViewHolder(viewHolder: RowPresenter.ViewHolder, row: Any) {
		viewHolder as ViewHolder
		row as DetailsOverviewRow
		val item = row.item

		// banner
		//todo hide banner view when none found, support multiple banners
		item.images.backdrops.firstOrNull()?.let {
			// Android doesn't crop automatically but Glide does
			// Picasso can also do this but doesn't read the XML attributes of the target view for it
			// so the way Glide does it is preferred to avoid duplicate settings
			Glide.with(viewHolder.view.context).load(it.url).into(viewHolder.banner)
		}

		// Logo
		item.images.logo?.load(viewHolder.view.context) { viewHolder.logo.setImageBitmap(it) }

		// Action adapter
		row.actions.forEach { action ->
			val holder = actionAdapter.createViewHolder(viewHolder.actions)
			viewHolder.actionViewHolders.add(holder)
			viewHolder.actions.addView(holder.view)

			actionAdapter.bindViewHolder(holder, action)
		}

		// poster
		item.images.primary?.load(viewHolder.view.context) { viewHolder.poster.setImageBitmap(it) }

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
			if (item.productionYear != null) {
				viewHolder.year.text = item.productionYear.toString()
				viewHolder.year.visibility = View.VISIBLE
			} else {
				viewHolder.year.visibility = View.GONE
			}

			if (item.officialRating != null) {
				viewHolder.officialRating.text = item.officialRating
				viewHolder.officialRating.visibility = View.VISIBLE
			} else {
				viewHolder.officialRating.visibility = View.GONE
			}

			if (item.communityRating != null) {
				viewHolder.communityRating.value = item.communityRating
				viewHolder.communityRating.visibility = View.VISIBLE
			} else {
				viewHolder.communityRating.visibility = View.GONE
			}

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

			if (item.durationTicks != null) {
				viewHolder.duration.text = TimeUtils.formatMillis(item.durationTicks / 10000)
				viewHolder.endsAt.text = DateFormat.getTimeFormat(viewHolder.view.context).format(System.currentTimeMillis() + (item.durationTicks - item.playbackPositionTicks) / 10000)
				viewHolder.durationInfo.visibility = View.VISIBLE
			} else {
				viewHolder.durationInfo.visibility = View.GONE
			}
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

	override fun onUnbindRowViewHolder(viewHolder: RowPresenter.ViewHolder) {
		viewHolder as ViewHolder

		// Unbind all actions
		viewHolder.actionViewHolders.removeAll {
			// Unbind action
			actionAdapter.unbindViewHolder(it)

			// Remove the view
			viewHolder.actions.removeView(it.view)

			// Return "true" to remove it from the list
			true
		}
	}
}
