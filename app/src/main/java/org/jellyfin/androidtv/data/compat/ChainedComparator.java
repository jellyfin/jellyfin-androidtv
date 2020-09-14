package org.jellyfin.androidtv.data.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Deprecated
public class ChainedComparator<T> implements Comparator<T> {
    private List<Comparator<T>> simpleComparators;

    public ChainedComparator(Comparator<T>... simpleComparators) {

        List<Comparator<T>> list = new ArrayList<>();

        for (Comparator<T> comparator : simpleComparators) {
            list.add(comparator);
        }
        this.simpleComparators = list;
    }

    public int compare(T o1, T o2) {
        for (Comparator<T> comparator : simpleComparators) {
            int result = comparator.compare(o1, o2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
