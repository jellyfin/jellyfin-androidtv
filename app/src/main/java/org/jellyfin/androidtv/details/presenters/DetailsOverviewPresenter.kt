package org.jellyfin.androidtv.details.presenters

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.text.bold
import androidx.leanback.widget.RowPresenter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.row_details_description.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.details.DetailsOverviewRow
import org.jellyfin.androidtv.details.GenreAdapter
import org.jellyfin.androidtv.details.actions.ActionAdapter
import org.jellyfin.androidtv.model.itemtypes.Episode
import org.jellyfin.androidtv.model.itemtypes.Movie
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.model.itemtypes.Ratable
import org.jellyfin.androidtv.ui.RecyclerViewSpacingDecoration
import org.jellyfin.androidtv.ui.widget.Rating
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.util.toHtmlSpanned
import org.jellyfin.apiclient.model.entities.MediaStreamType

class DetailsOverviewPresenter(private val context: Context) : RowPresenter() {
	private val actionAdapter = ActionAdapter()

	init {
		headerPresenter = null
		selectEffectEnabled = false
	}

	class ViewHolder(view: View) : RowPresenter.ViewHolder(view) {
		val banner: ImageView = view.details_description_banner
		val logo: ImageView = view.details_description_logo
		val poster: ImageView = view.details_description_poster
		val posterProgress: ProgressBar = view.details_description_poster_progress

		val actions: LinearLayout = view.details_description_actions
		val actionViewHolders = mutableListOf<ActionAdapter.ActionViewHolder>()

		val title: TextView = view.findViewById(R.id.details_description_title)
		val subtitle: TextView = view.findViewById(R.id.details_description_subtitle)

		val premiereDate: TextView = view.details_description_premiere_date
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
		row.backdrops.firstOrNull()?.let {
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
		row.primaryImage?.let { it.load(viewHolder.view.context) { viewHolder.poster.setImageBitmap(it) } }

		// title
		viewHolder.title.text = item.title

		// Original title
		if (item.titleOriginal != null && item.titleOriginal != item.title) {
			viewHolder.subtitle.text = item.titleOriginal
			viewHolder.subtitle.visibility = View.VISIBLE
		} else {
			viewHolder.subtitle.visibility = View.GONE
		}

		if (item is Movie) {
			item.productionYear.let {
				viewHolder.premiereDate.text = it.toString()
				viewHolder.premiereDate.visibility = View.VISIBLE
			}
		}

		if (item is Episode) {
			item.premiereDate?.let {
				val format = DateFormat.getDateFormat(context)
				viewHolder.premiereDate.text = format.format(it)
				viewHolder.premiereDate.visibility = View.VISIBLE
			}
		}

		// rating
		if (item is Ratable) {
			item.officialRating?.let {
				viewHolder.officialRating.text = it
				viewHolder.officialRating.visibility = View.VISIBLE
			}

			item.communityRating?.let {
				viewHolder.communityRating.value = it
				viewHolder.communityRating.visibility = View.VISIBLE
			}

			item.criticsRating?.let {
				viewHolder.criticsRating.value = it
				viewHolder.criticsRating.visibility = View.VISIBLE
			}
		}

		if (item is PlayableItem) {
			// Calculate progress in percentage (0 - 100)
			val progress = item.durationTicks?.let { item.playbackPositionTicks * 100.0 / it }

			if (progress != null && progress > 0) {
				viewHolder.posterProgress.visibility = View.VISIBLE
				viewHolder.posterProgress.max = 100
				viewHolder.posterProgress.progress = progress.toInt()
			} else {
				viewHolder.posterProgress.visibility = View.GONE
			}

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
		viewHolder.body.text = item.description?.toHtmlSpanned()
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
