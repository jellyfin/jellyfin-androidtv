package org.jellyfin.androidtv.ui.playback;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.ui.CaptionStyleCompat;

import org.jellyfin.androidtv.preference.UserPreferences;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Helper class to manage custom subtitle rendering with proper stacking of multiple cues.
 */
public class CustomSubtitleHelper {
    private final Context context;
    private final LinearLayout subtitleContainer;
    private final List<TextView> subtitleViews = new ArrayList<>();
    private CaptionStyleCompat captionStyle;
    private float textSize = 18f;

    public CustomSubtitleHelper(@NonNull Context context, @NonNull LinearLayout subtitleContainer, @NonNull UserPreferences userPreferences) {
        this.context = context;
        this.subtitleContainer = subtitleContainer;

        // Configure the subtitle style based on user preferences
        int strokeColor = userPreferences.get(UserPreferences.Companion.getSubtitleTextStrokeColor()).intValue();
        captionStyle = new CaptionStyleCompat(
                userPreferences.get(UserPreferences.Companion.getSubtitlesTextColor()).intValue(),
                userPreferences.get(UserPreferences.Companion.getSubtitlesBackgroundColor()).intValue(),
                Color.TRANSPARENT,
                Color.alpha(strokeColor) == 0 ? CaptionStyleCompat.EDGE_TYPE_NONE : CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                strokeColor,
                null
        );

        // Set text size based on user preferences, with a multiplier to make it larger
        float userSizePreference = userPreferences.get(UserPreferences.Companion.getSubtitlesTextSize());
        textSize = 0.0533f * userSizePreference * 500;

        Timber.d("Setting subtitle text size to %f (user preference: %f)", textSize, userSizePreference);
    }

    /**
     * Process and display subtitle cues.
     * @param cueGroup The cue group containing subtitle cues
     */
    public void onCues(CueGroup cueGroup) {
        List<Cue> cues = cueGroup != null ? cueGroup.cues : null;

        if (cues == null || cues.isEmpty()) {
            subtitleContainer.setVisibility(ViewGroup.GONE);
            return;
        }

        // Make sure the container is visible
        subtitleContainer.setVisibility(ViewGroup.VISIBLE);

        // Clear previous subtitles
        subtitleContainer.removeAllViews();

        Timber.d("Displaying %d subtitle cues", cues.size());

        // Ensure we have enough TextView instances
        while (subtitleViews.size() < cues.size()) {
            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            subtitleViews.add(textView);
        }

        // Add and configure each cue
        for (int i = 0; i < cues.size(); i++) {
            Cue cue = cues.get(i);
            TextView cueView = subtitleViews.get(i);

            // Set the text from the cue
            if (cue.text != null) {
                cueView.setText(cue.text);

                // Apply styling
                applyTextStyle(cueView);

                // Add to the container
                subtitleContainer.addView(cueView);
            }
        }
    }

    /**
     * Apply text styling to a subtitle TextView.
     * @param textView The TextView to style
     */
    private void applyTextStyle(TextView textView) {
        // Use a different unit for better TV display
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        textView.setTextColor(captionStyle.foregroundColor);

        // Make text bold for better visibility on TV
        textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);

        // Apply edge type (outline, drop shadow, etc.)
        switch (captionStyle.edgeType) {
            case CaptionStyleCompat.EDGE_TYPE_OUTLINE:
                // Increase outline thickness for better visibility
                textView.setShadowLayer(4, 0, 0, captionStyle.edgeColor);
                break;
            case CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW:
                // Increase shadow size for better visibility
                textView.setShadowLayer(4, 2, 2, captionStyle.edgeColor);
                break;
            case CaptionStyleCompat.EDGE_TYPE_NONE:
            default:
                textView.setShadowLayer(0, 0, 0, 0);
                break;
        }

        // Center the text
        textView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        textView.setGravity(Gravity.CENTER);

        // Set the background color
        textView.setBackgroundColor(captionStyle.backgroundColor);

        // Add some padding
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                context.getResources().getDisplayMetrics());
        textView.setPadding(padding, padding, padding, padding);

        // Add some margin between subtitle lines
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
        params.setMargins(0, 0, 0, padding / 2);
        textView.setLayoutParams(params);
    }
}