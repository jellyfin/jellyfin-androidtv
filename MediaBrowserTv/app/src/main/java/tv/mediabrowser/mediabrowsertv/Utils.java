package tv.mediabrowser.mediabrowsertv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.session.MediaController;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import java.net.URI;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dlna.profiles.AndroidProfile;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.session.PlaybackStartInfo;
import mediabrowser.model.session.PlaybackStopInfo;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    /**
     * Returns the screen/display size
     *
     * @param context
     * @return
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        return new Point(width, height);
    }

    /**
     * Shows an error dialog with a given text message.
     *
     * @param context
     * @param errorString
     */

    public static final void showErrorDialog(Context context, String errorString) {
        new AlertDialog.Builder(context).setTitle(R.string.error)
                .setMessage(errorString)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    /**
     * Shows a (long) toast
     *
     * @param context
     * @param msg
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a (long) toast.
     *
     * @param context
     * @param resourceId
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static byte[] encrypt(String x) throws Exception {
        java.security.MessageDigest d = null;
        d = java.security.MessageDigest.getInstance("SHA-1");
        d.reset();
        d.update(x.getBytes());
        return d.digest();
    }

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     *
     * @param millis
     * @return
     */
    public static String formatMillis(int millis) {
        String result = "";
        int hr = millis / 3600000;
        millis %= 3600000;
        int min = millis / 60000;
        millis %= 60000;
        int sec = millis / 1000;
        if (hr > 0) {
            result += hr + ":";
        }
        if (min >= 0) {
            if (min > 9) {
                result += min + ":";
            } else {
                result += "0" + min + ":";
            }
        }
        if (sec > 9) {
            result += sec;
        } else {
            result += "0" + sec;
        }
        return result;
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static int dpToPx(int dp, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setMaxWidth(500);
        options.setImageType(ImageType.Primary);
        options.setTag(item.getImageTags().get(ImageType.Primary));

        return apiClient.GetImageUrl(item, options);
    }
    public static String getBackdropImageUrl(BaseItemDto item, ApiClient apiClient) {
        if (item.getBackdropCount() > 0) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(1200);
            options.setImageType(ImageType.Backdrop);
            options.setImageIndex(0);
            options.setTag(item.getBackdropImageTags().get(0));
            return apiClient.GetImageUrl(item, options);
        } else {
            if (item.getParentBackdropImageTags() != null && item.getParentBackdropImageTags().size() > 0) {
                ImageOptions options = new ImageOptions();
                options.setMaxWidth(1200);
                options.setImageType(ImageType.Backdrop);
                options.setImageIndex(0);
                options.setTag(item.getParentBackdropImageTags().get(0));
                return apiClient.GetImageUrl(item.getParentBackdropItemId(), options);

            }
        }
        return null;
    }

    public static String GetFullName(BaseItemDto item) {
        switch (item.getType()) {
            case "Episode":
                return item.getSeriesName() + " " + item.getParentIndexNumber() + "x" + item.getIndexNumber() + " " + item.getName();
            default:
                return item.getName();
        }
    }

    public static void Play(BaseItemDto item, int position, VideoView view) {
        StreamBuilder builder = new StreamBuilder();
        VideoOptions options = new VideoOptions();
        ApiClient apiClient = TvApp.getApplication().getApiClient();
        options.setDeviceId(apiClient.getDeviceId());
        options.setItemId(item.getId());
        options.setMediaSources(item.getMediaSources());
        options.setProfile(new AndroidProfile());
        StreamInfo info = builder.BuildVideoItem(options);
        view.setVideoPath(info.ToUrl(apiClient.getApiUrl()));
        TvApp.getApplication().setCurrentPlayingItem(item);
        if (position > 0) {
            view.seekTo(position);
        }
        view.start();

        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId());
        startInfo.setPositionTicks((long) 0);
        apiClient.ReportPlaybackStartAsync(startInfo, new EmptyResponse());

    }

    public static void Stop(BaseItemDto item, long pos) {
        if (item != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            apiClient.ReportPlaybackStoppedAsync(info, new EmptyResponse());
            apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), new EmptyResponse());

        }

    }

    public static void Beep() {
        // send the tone to the "alarm" stream (classic beeps go there) with 50% volume
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); // 200 is duration in ms
    }

    public static void ReportProgress(BaseItemDto item, long position) {
        if (item != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(position);
            apiClient.ReportPlaybackProgressAsync(info, new EmptyResponse());

        }

    }
}
