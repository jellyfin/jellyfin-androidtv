package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextClock
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
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
	init {
		val inflater = LayoutInflater.from(context)
		val view = inflater.inflate(R.layout.clock_user_bug, null, false)
		this.addView(view)
		val clock = view.findViewById<TextClock>(R.id.clock)
		val showClock = get<UserPreferences>()[clockBehavior]

		clock.visibility = when (showClock) {
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
			val username = view.findViewById<View>(R.id.userName) as TextView
			username.text = TvApp.getApplication().currentUser!!.name
			val userImage = view.findViewById<View>(R.id.userImage) as ImageView
			if (TvApp.getApplication().currentUser!!.primaryImageTag != null) {
				Glide.with(context)
					.load(ImageUtils.getPrimaryImageUrl(TvApp.getApplication().currentUser, get()))
					.error(R.drawable.ic_user)
					.override(30, 30)
					.centerInside()
					.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
					.into(userImage)
			} else {
				userImage.setImageResource(R.drawable.ic_user)
			}
		}
	}
}
