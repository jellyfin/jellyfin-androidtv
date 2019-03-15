package org.jellyfin.androidtv.model.compat;

@Deprecated
public class FullTranscoderSupport implements ITranscoderSupport {
    public final boolean CanEncodeToAudioCodec(String codec) {
        return true;
    }
}