package org.jellyfin.androidtv.util;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import org.jellyfin.androidtv.TvApp;

import timber.log.Timber;

import static android.media.MediaFormat.MIMETYPE_VIDEO_HEVC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_H263;
import static android.media.MediaCodecInfo.CodecProfileLevel.*;

public class MediaCodecCapabilitiesTest  {
    private static final String TAG = "MediaCodecCapabilitiesTest";
    private static final int PLAY_TIME_MS = 30000;
    private static final int TIMEOUT_US = 1000000;  // 1 sec
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private final MediaCodecList mRegularCodecs =
            new MediaCodecList(MediaCodecList.REGULAR_CODECS);
    private final MediaCodecList mAllCodecs =
            new MediaCodecList(MediaCodecList.ALL_CODECS);
    private final MediaCodecInfo[] mRegularInfos =
            mRegularCodecs.getCodecInfos();
    private final MediaCodecInfo[] mAllInfos =
            mAllCodecs.getCodecInfos();

    public boolean supportsHevc() {
        return MediaUtils.checkDecoder(MIMETYPE_VIDEO_HEVC);
    }

    public boolean supportsHevcMain10() {
        return hasDecoder(MIMETYPE_VIDEO_HEVC, HEVCProfileMain10, HEVCMainTierLevel5);
    }

    private boolean checkDecoder(String mime, int profile, int level) {
        if (!hasDecoder(mime, profile, level)) {
            Timber.i("no %s decoder for profile %d and level %d", mime, profile, level);
            return false;
        }
        return true;
    }
    private boolean hasDecoder(String mime, int profile, int level) {
        return supports(mime, false /* isEncoder */, profile, level);
    }
    private boolean supports(
            String mime, boolean isEncoder, int profile, int level) {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (isEncoder != info.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mime);
                for (MediaCodecInfo.CodecProfileLevel pl : caps.profileLevels) {
                    if (pl.profile != profile) {
                        continue;
                    }
                    // H.263 levels are not completely ordered:
                    // Level45 support only implies Level10 support
                    if (mime.equalsIgnoreCase(MIMETYPE_VIDEO_H263)) {
                        if (pl.level != level && pl.level == H263Level45 && level > H263Level10) {
                            continue;
                        }
                    }
                    if (pl.level >= level) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
            }
        }
        return false;
    }
}
