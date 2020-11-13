package org.jellyfin.androidtv.util.apiclient;

import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (mediaSource == null) return Collections.emptyList();

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
