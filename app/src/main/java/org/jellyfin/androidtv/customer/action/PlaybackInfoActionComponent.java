package org.jellyfin.androidtv.customer.action;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;

import java.util.Map;

public interface PlaybackInfoActionComponent {
    Map<String, Map<String, String>> getVideoInfos();

    default void doClick(@NonNull Context context, @NonNull View view) {
        View playbackVideoInfoLayout = view.getRootView().findViewById(R.id.playback_video_info_layout);
        if (playbackVideoInfoLayout == null) {
            return;
        }

        if (View.VISIBLE == playbackVideoInfoLayout.getVisibility()) {
            playbackVideoInfoLayout.setVisibility(View.GONE);
            return;
        }

        TextView playbackInfo = playbackVideoInfoLayout.findViewById(R.id.playback_video_info);
        if (playbackInfo == null) {
            return;
        }

        String videoInfoString = getVideoInfoString();
        if (videoInfoString == null || videoInfoString.isEmpty()) {
            return;
        }

        playbackInfo.setText(videoInfoString);
        playbackVideoInfoLayout.setVisibility(View.VISIBLE);
    }

    default String getVideoInfoString(){
        Map<String, Map<String, String>> videoInfos = getVideoInfos();
        if (videoInfos == null || videoInfos.isEmpty()) {
            return null;
        }

        StringBuilder videoInfoString = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> labelMap : videoInfos.entrySet()) {
            String label = labelMap.getKey();
            Map<String, String> valueMaps = labelMap.getValue();
            videoInfoString.append(label);
            for (Map.Entry<String, String> values : valueMaps.entrySet()) {
                String key = values.getKey();
                String value = values.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }

                videoInfoString.append("\n\t")
                        .append(key)
                        .append(" ")
                        .append(value);
            }
            videoInfoString.append("\n");
        }
        return videoInfoString.toString();
    }

    default DisplayMetrics getResolutionRatio(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            Display display = context.getDisplay();
            display.getMetrics(displayMetrics);
            return displayMetrics;
        }
        if (context instanceof Activity) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            return displayMetrics;
        }
        return null;
    }
}
