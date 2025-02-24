package org.jellyfin.androidtv.customer.behavior;

import org.jellyfin.sdk.model.api.MediaStream;

import java.util.List;

public interface BestStreamSelector {
//    protected static Map<MediaStreamType, Integer> typeSort = new HashMap<>();
//    static {
//        typeSort.put(MediaStreamType.VIDEO, 1);
//        typeSort.put(MediaStreamType.AUDIO, 10);
//        typeSort.put(MediaStreamType.SUBTITLE, 20);
//        typeSort.put(MediaStreamType.EMBEDDED_IMAGE, 30);
//        typeSort.put(MediaStreamType.DATA, 40);
//        typeSort.put(MediaStreamType.LYRIC, 50);
//    }

    Integer getBestMatchStream(List<MediaStream> mediaStreams);
}
