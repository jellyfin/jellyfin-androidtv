package org.jellyfin.androidtv.model.compat;

@Deprecated
public interface ITranscoderSupport {
    boolean CanEncodeToAudioCodec(String codec);
}