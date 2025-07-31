package org.jellyfin.androidtv.ui.playback.overlay

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.leanback.widget.PlaybackSeekDataProvider
import org.jellyfin.androidtv.ui.composable.ThumbnailPreview

class ThumbnailPreviewHandler(
    private val seekProvider: PlaybackSeekDataProvider?,
    private val thumbnailComposeView: ComposeView?
) {
    
    private var currentBitmap by mutableStateOf<Bitmap?>(null)
    private var currentRequestIndex: Int = -1
    
    init {
        thumbnailComposeView?.setContent {
            ThumbnailPreview(bitmap = currentBitmap)
        }
    }

    fun updateThumbnailPreview(previewSeekPosition: Long) {
        if (seekProvider == null || thumbnailComposeView == null) return

        val thumbnailIndex = findClosestIndex(seekProvider.seekPositions, previewSeekPosition)
        
        currentRequestIndex = thumbnailIndex

        val thumbnailCallback = object : PlaybackSeekDataProvider.ResultCallback() {
            override fun onThumbnailLoaded(bitmap: Bitmap?, index: Int) {
                // Ignore old requests
                if (index != currentRequestIndex || bitmap == null) return

                showThumbnail(bitmap)
            }
        }

        seekProvider.getThumbnail(thumbnailIndex, thumbnailCallback)
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
        currentRequestIndex = -1
        currentBitmap = null 
    }

    fun isAvailable(): Boolean {
        return seekProvider != null && thumbnailComposeView != null
    }
} 