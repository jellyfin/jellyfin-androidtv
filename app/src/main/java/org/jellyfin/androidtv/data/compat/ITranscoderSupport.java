package org.jellyfin.androidtv.data.compat;

@Deprecated
public interface ITranscoderSupport {
    boolean CanEncodeToAudioCodec(String codec);
}
