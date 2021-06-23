package org.jellyfin.androidtv

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class JellyfinGlideModule : AppGlideModule() {
	override fun applyOptions(context: Context, builder: GlideBuilder) {
		builder.setDefaultRequestOptions(
			// Set default disk cache strategy
			RequestOptions()
				.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
		)

		// Silence image load errors
		builder.setLogLevel(Log.ERROR)
	}
}
