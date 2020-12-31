package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.toHtmlSpanned

class NextUpFragment(private val data: NextUpItemData) : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_next_up, container, false).apply {
			BackgroundManager.getInstance(activity).setBitmap(data.backdrop)

			findViewById<ImageView>(R.id.logo).setImageBitmap(data.logo)
			findViewById<ImageView>(R.id.image).setImageBitmap(data.thumbnail)
			findViewById<TextView>(R.id.title).text = data.title
			findViewById<TextView>(R.id.description).text = data.description?.toHtmlSpanned()

			findViewById<NextUpButtons>(R.id.fragment_next_up_buttons).apply {
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

		requireView().findViewById<NextUpButtons>(R.id.fragment_next_up_buttons).startTimer()
	}

	override fun onPause() {
		super.onPause()

		requireView().findViewById<NextUpButtons>(R.id.fragment_next_up_buttons).stopTimer()
	}
}
