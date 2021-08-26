package org.jellyfin.androidtv.util;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import timber.log.Timber;

public class MediaUtils {
    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    public static boolean checkDecoder(String... mimes) {
        boolean hasDecoder = hasCodecForMimes(false, mimes);
        if (!hasDecoder) {
            Timber.i("no decoder found");
        }
        return hasDecoder;
    }
    private static boolean hasCodecForMimes(boolean encoder, String[] mimes) {
        for (String mime : mimes) {
            if (!hasCodecForMime(encoder, mime)) {
                Timber.i("no %s for %s", encoder ? "encoder" : "decoder", mime);
                return false;
            }
        }
        return true;
    }
    private static boolean hasCodecForMime(boolean encoder, String mime) {
        for (MediaCodecInfo info : sMCL.getCodecInfos()) {
            if (encoder != info.isEncoder()) {
                continue;
            }
            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mime)) {
                    Timber.i("found codec %s for mime %s", info.getName(), mime);
                    return true;
                }
            }
        }
        return false;
    }

}
