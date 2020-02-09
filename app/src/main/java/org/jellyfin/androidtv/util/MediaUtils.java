package org.jellyfin.androidtv.util;

import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodecInfo;

import org.jellyfin.androidtv.TvApp;

/**
 * Created by spam on 7/15/2016.
 */
public class MediaUtils {
    private static final MediaCodecList sMCL = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

    public static boolean check(boolean result, String message) {
        if (!result) {
            TvApp.getApplication().getLogger().Info("%s", message);
        }
        return result;
    }
    public static boolean canDecode(MediaFormat format) {
        if (sMCL.findDecoderForFormat(format) == null) {
            TvApp.getApplication().getLogger().Info("no decoder for %s", format.toString());
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
                TvApp.getApplication().getLogger().Info("no %s for %s", encoder ? "encoder" : "decoder", mime);
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
                    TvApp.getApplication().getLogger().Info("found codec %s for mime %s", info.getName(), mime);
                    return true;
                }
            }
        }
        return false;
    }

}
