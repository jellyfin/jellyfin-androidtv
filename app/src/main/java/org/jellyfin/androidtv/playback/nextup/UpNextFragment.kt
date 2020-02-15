package org.jellyfin.androidtv.playback.nextup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import kotlinx.android.synthetic.main.fragment_upnext_row.*
import kotlinx.android.synthetic.main.fragment_upnext_row.view.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.toHtmlSpanned

class UpNextFragment(private val data: UpNextItemData) : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_upnext_row, container, false).apply {
			BackgroundManager.getInstance(activity).setBitmap(data.backdrop)

			image.setImageBitmap(data.thumbnail)
			title.text = data.title
			description.text = data.description?.toHtmlSpanned()

			fragment_upnext_buttons.apply {
				setPlayNextListener {
					startActivity(Intent(activity, PlaybackOverlayActivity::class.java))
					activity?.finish()
				}
				setCancelListener {
					activity?.finish()
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()

		fragment_upnext_buttons.startTimer()
	}

	override fun onPause() {
		super.onPause()

		fragment_upnext_buttons.stopTimer()
	}
}
