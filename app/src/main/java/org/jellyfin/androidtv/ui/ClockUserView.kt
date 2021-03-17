package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserPreferences.Companion.clockBehavior
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.getActivity
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@KoinApiExtension
class ClockUserView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs), KoinComponent {
	private var binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), null, false)

	init {
		this.addView(binding.root)
		val showClock = get<UserPreferences>()[clockBehavior]

		binding.clock.visibility = when (showClock) {
			ClockBehavior.ALWAYS -> VISIBLE
			ClockBehavior.NEVER -> GONE
			ClockBehavior.IN_VIDEO -> {
				if (context.getActivity() !is PlaybackOverlayActivity) GONE else VISIBLE
			}
			ClockBehavior.IN_MENUS -> {
				if (context.getActivity() is PlaybackOverlayActivity) GONE else VISIBLE
			}
		}

		if (!isInEditMode) {
			binding.userName.text = TvApp.getApplication().currentUser!!.name
			if (TvApp.getApplication().currentUser!!.primaryImageTag != null) {
				Glide.with(context)
					.load(ImageUtils.getPrimaryImageUrl(TvApp.getApplication().currentUser, get()))
					.error(R.drawable.ic_user)
					.override(30, 30)
					.centerInside()
					.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
					.into(binding.userImage)
			} else {
				binding.userImage.setImageResource(R.drawable.ic_user)
			}
		}
	}
}
