package org.jellyfin.androidtv.data.compat;

@Deprecated
public class FullTranscoderSupport implements ITranscoderSupport {
    public final boolean CanEncodeToAudioCodec(String codec) {
        return true;
    }
}
