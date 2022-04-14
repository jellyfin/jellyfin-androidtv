package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.getActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val userRepository by inject<UserRepository>()

	init {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> context.getActivity() is PlaybackOverlayActivity
			ClockBehavior.IN_MENUS -> context.getActivity() !is PlaybackOverlayActivity
		}

		val currentUser = userRepository.currentUser.value

		if (currentUser != null) {
			if (currentUser.primaryImageTag != null) {
				Glide.with(context)
					.load(ImageUtils.getPrimaryImageUrl(currentUser))
					.placeholder(R.drawable.ic_user)
					.centerInside()
					.circleCrop()
					.into(binding.clockUserImage)
			} else {
				binding.clockUserImage.setImageResource(R.drawable.ic_user)
			}
			binding.clockUserImage.isVisible = true
		} else {
			binding.clockUserImage.isVisible = false
		}
	}
}
