package org.jellyfin.androidtv.util.apiclient;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;

public class StreamHelper {
    public static MediaStream getMediaStream(MediaSourceInfo mediaSource, int index) {
        if (mediaSource.getMediaStreams() == null || mediaSource.getMediaStreams().size() == 0) return null;
        for (MediaStream stream : mediaSource.getMediaStreams()) {
            if (stream.getIndex() == index) return stream;
        }
        return null;
    }

    public static List<MediaStream> getSubtitleStreams(MediaSourceInfo mediaSource) {
        return getStreams(mediaSource, MediaStreamType.Subtitle);
    }

    public static List<MediaStream> getAudioStreams(MediaSourceInfo mediaSource) {
        return getStreams(mediaSource, MediaStreamType.Audio);
    }

    public static MediaStream getFirstAudioStream(BaseItemDto item) {
        if (item.getMediaSources() == null || item.getMediaSources().size() < 1) return null;
        List<MediaStream> streams = getAudioStreams(item.getMediaSources().get(0));
        if (streams == null || streams.size() < 1) return null;
        return streams.get(0);
    }

    public static List<MediaStream> getVideoStreams(MediaSourceInfo mediaSource) {
        return getStreams(mediaSource, MediaStreamType.Video);
    }

    public static List<MediaStream> getStreams(MediaSourceInfo mediaSource, MediaStreamType type) {
        ArrayList<MediaStream> streams = mediaSource.getMediaStreams();
        ArrayList<MediaStream> ret = new ArrayList<>();
        if (streams != null) {
            for (MediaStream stream : streams) {
                if (stream.getType() == type) {
                    ret.add(stream);
                }
            }
        }

        return ret;
    }
}
