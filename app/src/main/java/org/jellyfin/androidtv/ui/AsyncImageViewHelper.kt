package org.jellyfin.androidtv.ui

import org.jellyfin.sdk.model.api.BaseItemDto
import kotlin.math.floor


private const val EIGHT_SECONDS = 8000
fun AsyncImageView.photoAnimateWithZoomAndPan(duration: Long?, panEffectPercent: Float, zoomEffectPercent: Float, item: BaseItemDto?, screenWidth: Int?, screenHeight: Int?){
	if(duration == null || item == null || screenWidth == null || screenHeight == null || item.width == null || item.height == null)
		return
	val imageAspectRatio = item.width!!.toDouble() / item.height!!.toDouble()
	if(imageAspectRatio > 0.56 && imageAspectRatio < 2.0) {
		fun getRandomInt(min: Int, max: Int): Int {
			return floor(Math.random() * (max + 1 - min).toDouble()).toInt() + min
		}
		fun getRandomScaleValue(min: Int, max: Int): Float {
			return (getRandomInt(min, max).toFloat() / 100.0f * zoomEffectPercent) + 1.0f
		}
		fun getRandomPanValue(limit: Int): Float {
			return getRandomInt(-1 * limit, limit).toFloat() * panEffectPercent
		}
		fun getRandomSubDuration(duration: Long, min: Int, max: Int) : Long{
			return (duration * (getRandomInt(min, max).toFloat() / 100.0f)).toLong()
		}
		fun getScaledPanLimit(value: Int, scale: Float): Int{
			return ((value * (scale * 0.1f)) * panEffectPercent).toInt()
		}

		val use1PhaseActions = duration < EIGHT_SECONDS
		val randomAction = getRandomInt(0, 10)
		if (randomAction == 0) {
			//Static Image
			return
		}
		val randomScale1 = getRandomScaleValue(50,200)
		val xLimit = getScaledPanLimit((item.width ?: screenWidth),randomScale1)
		val yLimit = getScaledPanLimit((item.height ?: screenHeight), randomScale1)
		val rndX = getRandomPanValue(xLimit)
		val rndY = getRandomPanValue(yLimit)
		val actionDuration = (duration * 0.98f - crossFadeDuration.inWholeMilliseconds).toLong()
		val randomDuration = getRandomSubDuration(actionDuration,50,100)
		if (randomAction <= 2 || (use1PhaseActions && randomAction <= 3)) {
			//Zoom In
			zoomAndPanRunnable(this,0,0, 1.0f, 0.0f, 0.0f,randomDuration, randomScale1, rndX, rndY, false, 0)
		}
		else if (randomAction <= 4 || (use1PhaseActions && randomAction <= 6)) {
			//Zoom Out
			zoomAndPanRunnable(this,0,0, randomScale1, rndX, rndY, randomDuration, 1.0f, 0.0f, 0.0f, false,0)
		}
		else {
			val randomDuration1 = getRandomSubDuration(actionDuration,40,60)
			val randomDuration2 = actionDuration - randomDuration1
			val randomScale2 = getRandomScaleValue(50,200)
			val xLimit2 = getScaledPanLimit((item.width ?: screenWidth),randomScale2)
			val yLimit2 = getScaledPanLimit((item.height ?: screenHeight), randomScale2)
			val rndX2 = getRandomPanValue(xLimit2)
			val rndY2 = getRandomPanValue(yLimit2)
			if (randomAction <= 6 || use1PhaseActions) {
				//Zoom 1 to Zoom 2
				zoomAndPanRunnable(this, 0, 0, randomScale1, rndX, rndY, randomDuration, randomScale2, rndX2, rndY2, false, 0)
			}
			else if (randomAction <= 8) {
				//Zoom In and Zoom 2
				zoomAndPanRunnable(this, 0, randomDuration1, randomScale1, rndX, rndY, randomDuration2, randomScale2, rndX2, rndY2, false, 0)
			}
			else if (randomAction <= 10) {
				//Zoom 1 to Zoom 2 and Out
				zoomAndPanRunnable(this, 0, 0, randomScale1, rndX, rndY, randomDuration1, randomScale2, rndX2, rndY2, true, randomDuration2)
			}
		}
	}
	else if (duration >= EIGHT_SECONDS && (imageAspectRatio <= 0.56 || imageAspectRatio >= 2.0)){
		val scaleUp = getScaleUpToViewBounds(imageAspectRatio,screenWidth,screenHeight)
		if (scaleUp > 0.9f && scaleUp < 1.1f) {
			return
		}
		val xZoomPan = (screenWidth.toFloat() * scaleUp * 0.5f) - (screenWidth.toFloat() * 0.5f)
		val yZoomPan = (screenHeight.toFloat() * scaleUp * 0.5f) - (screenHeight.toFloat() * 0.5f)
		val holdOrZoomDuration = ((duration - crossFadeDuration.inWholeMilliseconds).toFloat() * 0.03f).toLong()
		val scrollDuration = ((duration - crossFadeDuration.inWholeMilliseconds).toFloat() * 0.8f).toLong()
		val randomizeDirection = floatArrayOf(-1f,1f).random()
		if (imageAspectRatio > 2.0f) {
			zoomAndPanRunnable(this,holdOrZoomDuration, holdOrZoomDuration, scaleUp, randomizeDirection*xZoomPan, 0.0f, scrollDuration, scaleUp,-1*randomizeDirection*xZoomPan,0.0f, true, holdOrZoomDuration)
		}
		else if (imageAspectRatio <= 0.56f) {
			zoomAndPanRunnable(this,holdOrZoomDuration,holdOrZoomDuration, scaleUp, 0.0f, -1*randomizeDirection*yZoomPan, scrollDuration, scaleUp,0.0f, randomizeDirection*yZoomPan, true, holdOrZoomDuration)
		}
	}
}

private fun getScaleUpToViewBounds(imageAspectRatio: Double, screenWidth: Int, screenHeight: Int): Float {
	var scaleUp = 1.0f
	val screenAspectRatio = screenWidth.toDouble() / screenHeight.toDouble()
	if(imageAspectRatio >= 2.0){
		scaleUp = (imageAspectRatio / screenAspectRatio).toFloat()
	}
	else if (imageAspectRatio <= 0.56){
		scaleUp = (screenAspectRatio / imageAspectRatio).toFloat()
	}
	return scaleUp
}

private fun zoomAndPanRunnable(view:AsyncImageView, startDelay: Long, duration1: Long, scale1: Float, x1: Float, y1: Float, duration2: Long, scale2: Float, x2: Float, y2: Float, resetZoomAndPan: Boolean, resetDuration: Long) {
	val resetZoomAndPanAction = Runnable{
		view.animate()
			.setDuration(resetDuration)
			.scaleY(1.0f).scaleX(1.0f)
			.x(0.0f).y(0.0f)
	}
	val panAction = Runnable {
		view.animate()
			.setDuration(duration2)
			.scaleY(scale2).scaleX(scale2)
			.x(x2).y(y2)
			.withEndAction(if(resetZoomAndPan) resetZoomAndPanAction else null)
	}
	view.animate()
		.setStartDelay(startDelay)
		.setDuration(duration1)
		.scaleY(scale1).scaleX(scale1)
		.x(x1).y(y1)
		.withEndAction(panAction)
}
