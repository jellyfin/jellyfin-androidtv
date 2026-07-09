package org.jellyfin.androidtv.ui.playback;

public final class PlaybackPositionTracker {
    private long lastStablePosition = -1;

    public void reset(long position) {
        lastStablePosition = normalizePosition(position);
    }

    public void updateFromPlayerPosition(long position) {
        if (position >= 0 && (position > 0 || lastStablePosition <= 0)) {
            lastStablePosition = position;
        }
    }

    public void updateFromSeekPosition(long position) {
        if (position >= 0) {
            lastStablePosition = position;
        }
    }

    public long getRecoverablePosition(long currentPosition, long pendingSeekPosition, boolean preferPendingSeek) {
        long normalizedCurrentPosition = normalizePosition(currentPosition);
        long normalizedPendingSeekPosition = normalizePosition(pendingSeekPosition);

        if (preferPendingSeek && normalizedPendingSeekPosition >= 0) {
            return normalizedPendingSeekPosition;
        }

        if (lastStablePosition > 0) {
            return lastStablePosition;
        }

        if (normalizedCurrentPosition > 0) {
            return normalizedCurrentPosition;
        }

        if (normalizedPendingSeekPosition >= 0) {
            return normalizedPendingSeekPosition;
        }

        return 0;
    }

    private long normalizePosition(long position) {
        return position >= 0 ? position : -1;
    }
}
