package org.jellyfin.androidtv.util

import android.content.Context
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.leanback.app.BackgroundManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import timber.log.Timber

fun BackgroundManager.drawable(
	context: Context,
	url: String,
	widthPixels: Int,
	heightPixels: Int
) {
	Glide.with(context)
		.load(url)
		.override(widthPixels, heightPixels)
		.centerCrop()
		.listener(object : RequestListener<Drawable?> {
			override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
				Timber.e(e)
				return false
			}

			override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
				val filter: ColorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.DST_OVER)
				resource?.colorFilter = filter
				resource?.alpha = 50
				drawable = resource
				return true
			}
		}).submit()
}
