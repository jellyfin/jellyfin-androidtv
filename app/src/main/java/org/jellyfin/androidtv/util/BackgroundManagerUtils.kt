package org.jellyfin.androidtv.util

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.leanback.app.BackgroundManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/** Created by Oriol on 30/05/20. **/

fun BackgroundManager.drawable(
	activity: Activity,
	url: String,
	skipMemoryCache: Boolean = true,
	transformation: BitmapTransformation = CenterInside(),
	widthPixels: Int,
	heightPixels: Int
) {

	Glide.with(activity)
		.load(url)
		.skipMemoryCache(skipMemoryCache)
		.override(widthPixels, heightPixels)
		.transform(transformation)
		.listener(object : RequestListener<Drawable?> {
			override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
				return false
			}

			override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
				drawable = resource
				return false
			}
		}).submit()


}
