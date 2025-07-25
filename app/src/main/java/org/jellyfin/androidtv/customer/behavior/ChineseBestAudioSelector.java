package org.jellyfin.androidtv.customer.behavior;

import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

/**
 * 国语选择器
 */
public class ChineseBestAudioSelector implements BestStreamSelector {
    protected static final int BEST_SORT = 1;
    protected static final int DEFAULT_SORT = 10000;
    protected static final int LOW_SORT = 100000;
    protected static Map<String, Integer> bestMatchSort;
    static {
        bestMatchSort = new LinkedHashMap<>();
        bestMatchSort.put("国语", BEST_SORT); // 国语
        bestMatchSort.put("普通话", BEST_SORT);
        bestMatchSort.put("mandarin", BEST_SORT); // 普通话
        bestMatchSort.put("cantonese", 100); // 广东
        bestMatchSort.put("中文", 500);
        bestMatchSort.put("粤语", 1000);
        bestMatchSort.put("dolby", LOW_SORT); // 杜比音降序
    }

    @Override
    public Integer getBestMatchStream(List<MediaStream> mediaStreams) {
        if (mediaStreams == null || mediaStreams.isEmpty()) {
            return null;
        }

        MediaStream bastMatchMediaStream = mediaStreams
                .stream()
                .filter(mediaStream -> MediaStreamType.AUDIO.equals(mediaStream.getType()))
                .sorted((a, b) -> {
                    int aLevel = getBestSortLevel(Objects.nonNull(a.getDisplayTitle()) ? a.getDisplayTitle() : a.getTitle());
                    int bLevel = getBestSortLevel(Objects.nonNull(b.getDisplayTitle()) ? b.getDisplayTitle() : b.getTitle());
                    return Integer.compare(aLevel, bLevel);
                })
                .findFirst()
                .orElse(null);

        if (bastMatchMediaStream == null) {
            return null;
        }

        int i = mediaStreams.indexOf(bastMatchMediaStream);
        Timber.d("getBestMatchStream %s %d", bastMatchMediaStream.getDisplayTitle(),  i);
        return i;
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
