package org.jellyfin.androidtv.util;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import timber.log.Timber;

public class MediaUtils {
    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    public static boolean check(boolean result, String message) {
        if (!result) {
            Timber.i("%s", message);
        }
        return result;
    }
    public static boolean canDecode(MediaFormat format) {
        if (sMCL.findDecoderForFormat(format) == null) {
            Timber.i("no decoder for %s", format.toString());
            return false;
        }
        return true;
    }
    public static boolean checkDecoder(String... mimes) {
        return check(hasCodecForMimes(false /* encoder */, mimes), "no decoder found");
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
