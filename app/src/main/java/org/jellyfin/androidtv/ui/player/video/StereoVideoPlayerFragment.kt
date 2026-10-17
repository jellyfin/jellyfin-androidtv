package org.jellyfin.androidtv.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.compose.content
import org.jellyfin.androidtv.ui.base.BaseScreen

class StereoVideoPlayerFragment : VideoPlayerFragment() {
	companion object {
		const val EXTRA_VIDEO_3D_FORMAT: String = "video3dFormat"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		BaseScreen {
			StereoVideoPlayerScreen()
		}
	}
}
