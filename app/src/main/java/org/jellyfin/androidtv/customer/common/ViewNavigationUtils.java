package org.jellyfin.androidtv.customer.common;

import android.view.View;

import java.util.List;

public class ViewNavigationUtils {
    public static void listViewsNavigation(List<View> views, boolean horizontal) {
        if (views.isEmpty())
        {
            return;
        }

        View preView = null;
        for (View view : views) {
            if (view == null)
            {
                continue;
            }

            if (preView != null)
            {
                viewPreNext(preView, view, horizontal, true);
            }

            preView = view;
        }

        View firstView = views.get(0);
        if (firstView != preView)
        {
            viewPreNext(preView, firstView, horizontal, true);
        }
    }

    public static void viewIndexNext(List<View> views, View indexView, int index) {
        int size = views.size();
        View preView = views.get((index - 1 + size) % size);
        View nextView = views.get((index + 1) % size);

        viewPreNext(preView, indexView, false, false);
        viewPreNext(indexView, nextView, false, false);
    }

    public static void viewPreNext(View pre, View next, boolean horizontal, boolean loop) {
        if (horizontal) {
            pre.setNextFocusRightId(next.getId());
            if (loop) {
                next.setNextFocusLeftId(pre.getId());
            }
            return;
        }

        pre.setNextFocusDownId(next.getId());
        if (loop) {
            next.setNextFocusUpId(pre.getId());
        }
    }

    public static void clearAllNavigation(View button) {
        button.setNextFocusUpId(button.getId());
        button.setNextFocusDownId(button.getId());
        button.setNextFocusLeftId(button.getId());
        button.setNextFocusRightId(button.getId());
    }

    public static void viewNextPre(View pre, View next, boolean horizontal, boolean loop) {
        if (horizontal) {
            next.setNextFocusLeftId(pre.getId());
            if (loop) {
                pre.setNextFocusRightId(next.getId());
            }
            return;
        }
        next.setNextFocusUpId(pre.getId());
        if (loop) {
            pre.setNextFocusDownId(next.getId());
        }
    }
}
