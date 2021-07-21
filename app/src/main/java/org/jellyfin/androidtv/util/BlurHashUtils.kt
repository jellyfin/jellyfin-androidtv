@file:JvmName("BlurHashUtils")

package org.jellyfin.androidtv.util

import android.graphics.Bitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun createBlurHashDrawable(
	lifecycleOwner: LifecycleOwner,
	blurHash: String?,
	width: Int,
	height: Int,
	callback: (bitmap: Bitmap?) -> Unit,
) = lifecycleOwner.lifecycleScope.launch {
	// Create blurhash in IO
	val bitmap = if (blurHash.isNullOrEmpty() || width <= 0 || height <= 0) null
	else withContext(Dispatchers.IO) { BlurHashDecoder.decode(blurHash, width, height) }

	// Execute callback on main
	withContext(Dispatchers.Main) {
		callback(bitmap)
	}
}
