package org.jellyfin.androidtv.data.compat;

import java.util.Comparator;

@Deprecated
public abstract class BaseStreamInfoSorter implements Comparator<StreamInfo> {
    protected abstract int getValue(StreamInfo info);

    @Override
    public int compare(StreamInfo lhs, StreamInfo rhs) {
        return Integer.compare(getValue(lhs), getValue(rhs));
    }
}
