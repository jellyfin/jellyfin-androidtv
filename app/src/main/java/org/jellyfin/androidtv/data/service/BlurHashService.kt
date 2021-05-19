package org.jellyfin.androidtv.data.service

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.util.BlurHashDecoder

class BlurHashService(
	private val context: Context
) {
	private val scope = MainScope()

	fun decodeBlurHashDrawable(
		blurHash: String,
		width: Int,
		height: Int,
		response: (drawable: BitmapDrawable) -> Unit
	) {
		scope.launch {
			var bitmapDrawable: BitmapDrawable
			withContext(Dispatchers.IO) {
				val bitmap = BlurHashDecoder.decode(
					blurHash,
					width,
					height
				)
				bitmapDrawable = BitmapDrawable(context.resources, bitmap)
			}
			response(bitmapDrawable)
		}
	}
}
