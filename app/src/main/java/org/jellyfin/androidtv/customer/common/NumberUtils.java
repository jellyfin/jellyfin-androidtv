package org.jellyfin.androidtv.customer.common;

import java.util.Objects;

/**
 * @author fengymi
 * date 2024-09-19 14:46
 */
public class NumberUtils {

    /**
     * Comparable对象比较，支持null
     * @param left left
     * @param right right
     * @return 对比结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compare(Comparable<?> left, Comparable<?> right) {
        if (Objects.equals(left, right)) {
            return 0;
        }

        if (left == null) {
            return -1;
        }

        if (right == null) {
            return 1;
        }

        return ((Comparable) left).compareTo(right);

    }


    public static Number numberOfScale(Number number, int Scale) {
        if (number == null) {
            return null;
        }

        if (number.intValue() == 0) {
            return null;
        }

        if (number instanceof Double) {
            return number.doubleValue() / Scale;
        }

        if (number instanceof Float) {
            return number.floatValue() / Scale;
        }

        if (number instanceof Long) {
            return number.longValue() / Scale;
        }

        if (number instanceof Integer) {
            return number.intValue() / Scale;
        }
        return number;
    }
}
