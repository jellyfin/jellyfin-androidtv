package org.jellyfin.androidtv.playback;

import androidx.leanback.widget.PlaybackSeekDataProvider;

public class CustomSeekProvider extends PlaybackSeekDataProvider {

    @Override
    public long[] getSeekPositions() {
        return new long[5];
    }
}
