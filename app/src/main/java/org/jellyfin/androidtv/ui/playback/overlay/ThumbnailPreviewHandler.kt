package org.jellyfin.androidtv.ui.playback.overlay

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.leanback.widget.PlaybackSeekDataProvider
import org.jellyfin.androidtv.ui.composable.ThumbnailPreview
import java.util.concurrent.atomic.AtomicInteger

class ThumbnailPreviewHandler(
    private val seekProvider: PlaybackSeekDataProvider?,
    private val thumbnailComposeView: ComposeView?
) {
    private var currentBitmap by mutableStateOf<Bitmap?>(null)
    private val requestNumber = AtomicInteger(0)
    private val lastShownRequestNumber = AtomicInteger(0)

    companion object {
        private const val MAX_REQUEST_AGE = 20
    }

    init {
        thumbnailComposeView?.setContent {
            ThumbnailPreview(bitmap = currentBitmap)
        }
    }

    fun updateThumbnailPreview(previewSeekPosition: Long) {
        val currentRequestNumber = requestNumber.incrementAndGet()
        
        val thumbnailCallback = object : PlaybackSeekDataProvider.ResultCallback() {
            override fun onThumbnailLoaded(bitmap: Bitmap?, index: Int) {
                if (bitmap == null) return
                
                // Ignore requests that are older than what we last showed
                if (currentRequestNumber <= lastShownRequestNumber.get()) return
                
                // Ignore requests that are too old
                if (currentRequestNumber < requestNumber.get() - MAX_REQUEST_AGE) return
                
                lastShownRequestNumber.set(currentRequestNumber)
                showThumbnail(bitmap)
            }
        }

        val thumbnailIndex = findClosestIndex(seekProvider?.seekPositions ?: LongArray(0), previewSeekPosition)
        seekProvider?.getThumbnail(thumbnailIndex, thumbnailCallback)
    }

    private fun findClosestIndex(positions: LongArray, targetPosition: Long): Int {
        if (positions.isEmpty() || targetPosition < 0) return 0
        
        return positions.indexOfFirst { it >= targetPosition }
            .takeIf { it >= 0 } ?: (positions.size - 1).coerceAtLeast(0)
    }

    fun showThumbnail(bitmap: Bitmap) {
        currentBitmap = bitmap
    }

    fun hideThumbnailPreview() {
        currentBitmap = null 
    }

    fun isAvailable(): Boolean {
        return seekProvider != null && thumbnailComposeView != null
    }
} 