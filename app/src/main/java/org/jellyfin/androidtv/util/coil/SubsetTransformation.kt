package org.jellyfin.androidtv.util.coil

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

class SubsetTransformation(
	private val x: Int,
	private val y: Int,
	private val width: Int,
	private val height: Int,
) : Transformation {
	override val cacheKey: String = "$x,$y,$width,$height"

	override suspend fun transform(
		input: Bitmap,
		size: Size,
	): Bitmap = Bitmap.createBitmap(input, x, y, width, height)
}
