package org.jellyfin.androidtv.util;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    public static void addBlockText(Context context, LinearLayout layout, String text, int size, int textColor, int backgroundRes) {
        TextView view = new TextView(context);
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setText(" " + text + " ");
        view.setBackgroundResource(backgroundRes);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        params.setMargins(0, Utils.convertDpToPixel(context, -2), 0, 0);
        view.setLayoutParams(params);
        layout.addView(view);
    }
}
