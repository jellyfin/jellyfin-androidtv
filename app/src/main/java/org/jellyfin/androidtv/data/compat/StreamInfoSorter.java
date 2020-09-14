package org.jellyfin.androidtv.data.compat;

import java.util.ArrayList;
import java.util.Collections;

@Deprecated
public class StreamInfoSorter
{
    public static ArrayList<StreamInfo> SortMediaSources(ArrayList<StreamInfo> streams, Integer maxBitrate)
    {
        ChainedComparator<StreamInfo> comparator = new ChainedComparator<>(
                new StreamInfoSorterComparator(0),
                new StreamInfoSorterComparator(1),
                new StreamInfoSorterComparator(2)
        );

        Collections.sort(streams, comparator);
        return streams;
    }
}
