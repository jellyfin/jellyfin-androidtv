package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.toHtmlSpanned

class NextUpFragment(private val data: NextUpItemData) : Fragment() {
	private lateinit var binding: FragmentNextUpBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentNextUpBinding.inflate(inflater, container, false)

		BackgroundManager.getInstance(activity).setBitmap(data.backdrop)

		binding.logo.setImageBitmap(data.logo)
		binding.image.setImageBitmap(data.thumbnail)
		binding.title.text = data.title
		binding.description.text = data.description?.toHtmlSpanned()

		binding.fragmentNextUpButtons.apply {
			setPlayNextListener {
				startActivity(Intent(activity, PlaybackOverlayActivity::class.java))
				activity?.finish()
			}
			setCancelListener {
				activity?.finish()
			}
		}

		return binding.root
	}

	override fun onResume() {
		super.onResume()

		binding.fragmentNextUpButtons.startTimer()
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentNextUpButtons.stopTimer()
	}
}
