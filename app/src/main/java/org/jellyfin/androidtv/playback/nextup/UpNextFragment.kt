package org.jellyfin.androidtv.playback.nextup

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_upnext_row.*
import kotlinx.android.synthetic.main.fragment_upnext_row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.androidtv.util.toHtmlSpanned
import org.jellyfin.apiclient.model.dto.ImageOptions

//todo: proper way to get data & create view
class UpNextFragment : Fragment() {
	private var data: UpNextItemData? = null
	private var countdownTimer: CountDownTimer? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		GlobalScope.launch(Dispatchers.Main) {
			data = loadItem("e4f090de63eaf5fb5d34df6a7f8e504e")
//			data = loadItem("aa170e9f519e71d17724f1c8a045c027")
//			data = loadItem("fa78ba27f0460c1daa7622120dbdea0b")

			updateUi(data!!)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_upnext_row, container, false).apply {
			image.setOnClickListener {
				stopCountdown()
				playItem()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		image.requestFocus()
	}

	fun updateUi(data: UpNextItemData) {
		GlobalScope.launch(Dispatchers.Main) {
			data.backdrop.let {
				val bitmap = withContext(Dispatchers.IO) { Picasso.with(activity).load(it).get() }
				BackgroundManager.getInstance(activity).setBitmap(bitmap)
			}

			Picasso.with(context).load(data.thumbnail).into(image)
			title.text = data.title
			description.text = data.description?.toHtmlSpanned()

			startCountdown()
		}
	}

	private suspend fun loadItem(id: String) = withContext(Dispatchers.IO) {
		val item = TvApp.getApplication().apiClient.getItem(id) ?: return@withContext null

		val backdrop = TvApp.getApplication().apiClient.GetBackdropImageUrls(item, ImageOptions()).firstOrNull()
		val thumbnail = TvApp.getApplication().apiClient.GetImageUrl(item, ImageOptions())

		//todo improve "title" logic
		val title = if (item.parentIndexNumber != null && item.name != null)
			"${item.parentIndexNumber}. ${item.name}"
		else if (item.name != null)
			item.name
		else if (item.parentIndexNumber != null)
			"Episode" + item.parentIndexNumber
		else ""

		UpNextItemData(
			item.id,
			title,
			item.overview,
			backdrop,
			thumbnail
		)
	}

	private fun startCountdown() {
		val duration = 30 * 1000L // 5 seconds

		// Cancel current timer if one is already set
		countdownTimer?.cancel()

		// Set progress bar visible
		upnext_countdown.visibility = View.VISIBLE

		// Create timer
		countdownTimer = object : CountDownTimer(duration, 1) {
			override fun onTick(millisUntilFinished: Long) {
				upnext_countdown.max = duration.toInt()
				upnext_countdown.progress = millisUntilFinished.toInt()
			}

			override fun onFinish() {
				playItem()
			}
		}.start()
	}

	fun stopCountdown() {
		countdownTimer?.cancel()
		countdownTimer = null

		upnext_countdown.visibility = View.GONE
	}

	fun isCountdownActive() = countdownTimer != null

	private fun playItem() {
		stopCountdown()

		val intent = Intent(activity, PlaybackOverlayActivity::class.java)
		startActivity(intent)
		activity?.finish()
	}
}
