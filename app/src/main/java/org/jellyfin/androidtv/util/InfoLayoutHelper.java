package org.jellyfin.androidtv.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import org.jellyfin.androidtv.ui.browsing.composable.inforow.BaseItemInfoRowView;
import org.jellyfin.sdk.model.api.BaseItemDto;

public class InfoLayoutHelper {
    public static void addInfoRow(Context context, BaseItemDto item, LinearLayout layout, boolean includeRuntime) {
        // Find existing BaseItemInfoRowView or create a new one
        BaseItemInfoRowView baseItemInfoRowView = null;

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);

            if (child instanceof BaseItemInfoRowView) {
                baseItemInfoRowView = (BaseItemInfoRowView) child;
                break;
            }
        }

        if (baseItemInfoRowView == null) {
            baseItemInfoRowView = new BaseItemInfoRowView(context);
            layout.addView(baseItemInfoRowView);
        }

        // Update item info
        baseItemInfoRowView.setItem(item);
        baseItemInfoRowView.setIncludeRuntime(includeRuntime);
    }
}
