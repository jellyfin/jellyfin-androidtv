package org.jellyfin.androidtv.util;

import static org.koin.java.KoinJavaComponent.get;

import android.app.Activity;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer;
import org.jellyfin.androidtv.ui.playback.ExternalPlayerActivity;
import org.jellyfin.androidtv.ui.playback.PlaybackOverlayActivity;
import org.jellyfin.apiclient.model.dto.BaseItemType;

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


    public static boolean useExternalPlayer(BaseItemType itemType) {
        UserPreferences userPreferences = get(UserPreferences.class);
        switch (itemType) {
            case Movie:
            case Episode:
            case Video:
            case Series:
            case Recording:
                return userPreferences.get(UserPreferences.Companion.getVideoPlayer()) == PreferredVideoPlayer.EXTERNAL;
            case TvChannel:
            case Program:
                return userPreferences.get(UserPreferences.Companion.getLiveTvVideoPlayer()) == PreferredVideoPlayer.EXTERNAL;
            default:
                return false;
        }
    }

    @NonNull
    public static Class<? extends Activity> getPlaybackActivityClass(BaseItemType itemType) {
        return useExternalPlayer(itemType) ? ExternalPlayerActivity.class : PlaybackOverlayActivity.class;
    }
}
