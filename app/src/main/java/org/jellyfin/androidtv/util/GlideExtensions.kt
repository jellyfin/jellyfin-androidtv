@file:JvmName("GlideUtils")
package org.jellyfin.androidtv.util

import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import org.jellyfin.androidtv.data.service.BlurHashService

fun RequestBuilder<Drawable>.blurHashPlaceholder(
	blurHashService: BlurHashService,
	blurHash: String,
	width: Int,
	height: Int,
	response: (requestBuilder: RequestBuilder<Drawable>) -> Unit
) {
	if (blurHash.isNotEmpty() && width != 0 && height != 0) {
		blurHashService.decodeBlurHashDrawable(
			blurHash,
			width,
			height
		) { drawable ->
			this@blurHashPlaceholder.placeholder(drawable)
			response(this@blurHashPlaceholder)
		}
	} else {
		response(this@blurHashPlaceholder)
	}
}
