package org.jellyfin.androidtv.customer.behavior;

import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国语选择器
 */
public class ChineseBestAudioSelector implements BestStreamSelector {
    protected static final int BEST_SORT = 1;
    protected static final int DEFAULT_SORT = 10000;
    protected static Map<String, Integer> bestMatchSort;
    static {
        bestMatchSort = new LinkedHashMap<>();
        bestMatchSort.put("国语", BEST_SORT); // 国语
        bestMatchSort.put("普通话", BEST_SORT);
        bestMatchSort.put("mandarin", BEST_SORT); // 普通话
        bestMatchSort.put("cantonese", 100); // 广东
        bestMatchSort.put("中文", 500);
        bestMatchSort.put("粤语", 1000);
    }

    @Override
    public Integer getBestMatchStream(List<MediaStream> mediaStreams) {
        if (mediaStreams == null || mediaStreams.isEmpty()) {
            return null;
        }

        int sortLevel = DEFAULT_SORT;
        Integer bestIndex = null;
        for (MediaStream mediaStream : mediaStreams) {
            if (!MediaStreamType.AUDIO.equals(mediaStream.getType())) {
                continue;
            }

            String title = mediaStream.getTitle();
            int bestSortLevel = getBestSortLevel(title);
            if (bestSortLevel == BEST_SORT) {
                return mediaStream.getIndex();
            }

            if (sortLevel > bestSortLevel) {
                sortLevel = bestSortLevel;
                bestIndex = mediaStream.getIndex();
            }
        }
        return bestIndex;
    }

    protected int getBestSortLevel(String title) {
        if (title == null || title.isEmpty()) {
            return DEFAULT_SORT;
        }

        for (Map.Entry<String, Integer> titleLevel : bestMatchSort.entrySet()) {
            if (title.toLowerCase().contains(titleLevel.getKey())) {
                return titleLevel.getValue();
            }
        }
        return DEFAULT_SORT;
    }
}
