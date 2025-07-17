package org.jellyfin.androidtv.ui.playback.overlay;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import androidx.leanback.widget.PlaybackSeekDataProvider;

public class ThumbnailPreviewHandler {
    
    private final PlaybackSeekDataProvider seekProvider;
    private final ImageView thumbnailView;
    private final long skipForwardLength;
    
    public ThumbnailPreviewHandler(PlaybackSeekDataProvider seekProvider, 
                                   ImageView thumbnailView,
                                   long skipForwardLength) {
        this.seekProvider = seekProvider;
        this.thumbnailView = thumbnailView;
        this.skipForwardLength = skipForwardLength;
    }
    
    public void updateThumbnailPreview(long previewSeekPosition) {
        if (seekProvider == null || thumbnailView == null) return;

        int thumbnailIndex = (int) (previewSeekPosition / skipForwardLength);

        PlaybackSeekDataProvider.ResultCallback thumbnailCallback = new PlaybackSeekDataProvider.ResultCallback() {
            @Override
            public void onThumbnailLoaded(Bitmap bitmap, int index) {
                if (bitmap == null) return;

                thumbnailView.setImageBitmap(bitmap);
                showThumbnailPreview();
            }
        };

        seekProvider.getThumbnail(thumbnailIndex, thumbnailCallback);
    }
    
    public void showThumbnailPreview() {
        if (thumbnailView == null) return;

        thumbnailView.setVisibility(View.VISIBLE);
    }
    
    public void hideThumbnailPreview() {
        if (thumbnailView == null) return;
        
        thumbnailView.setVisibility(View.GONE);
        thumbnailView.setImageBitmap(null); // Clear the bitmap to free memory
    }
    
    public boolean isAvailable() {
        return seekProvider != null && thumbnailView != null;
    }
} 