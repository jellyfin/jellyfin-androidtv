package tv.mediabrowser.mediabrowsertv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.session.MediaController;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.model.dlna.StreamBuilder;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dlna.VideoOptions;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.entities.MediaType;
import mediabrowser.model.querying.EpisodeQuery;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.session.PlayMethod;
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

    private static String[] ThumbFallbackTypes = new String[] {"Episode"};

    public static Double getImageAspectRatio(BaseItemDto item) {
        if (Arrays.asList(ThumbFallbackTypes).contains(item.getType())) {
            if (item.getPrimaryImageAspectRatio() != null) return item.getPrimaryImageAspectRatio();
            if (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null) return 1.777777;
        }

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : .72222;
    }

    public static String getPrimaryImageUrl(BaseItemPerson item, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxWidth(300);
        options.setImageType(ImageType.Primary);
        return apiClient.GetPersonImageUrl(item.getName().replace(" ","%20"), options);
    }

    private static String[] ProgressIndicatorTypes = new String[] {"Episode", "Movie", "MusicVideo", "Video"};

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, Boolean showWatched) {
        ImageOptions options = new ImageOptions();
        String itemId = item.getId();
        String imageTag = item.getImageTags().get(ImageType.Primary);
        ImageType imageType = ImageType.Primary;
        if (item.getType().equals("Episode") && imageTag == null) {
            //try for thumb of season or series
            imageType = ImageType.Thumb;
            if (item.getParentThumbImageTag() != null) {
                imageTag = item.getParentThumbImageTag();
                itemId = item.getParentThumbItemId();
            } else {
                imageTag = item.getSeriesThumbImageTag();
                itemId = item.getSeriesId();
            }
        }
        if (item.getType().equals("Season") && imageTag == null) {
            imageTag = item.getSeriesPrimaryImageTag();
            itemId = item.getSeriesId();
        }
        options.setMaxWidth(320);
        options.setImageType(imageType);
        UserItemDataDto userData = item.getUserData();
        if (userData != null) {
            if (Arrays.asList(ProgressIndicatorTypes).contains(item.getType()) && userData.getPlayedPercentage() != null
                    && userData.getPlayedPercentage() > 0 && userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
            if (showWatched) options.setAddPlayedIndicator(userData.getPlayed());

        }

        options.setTag(imageTag);

        return apiClient.GetImageUrl(itemId, options);
    }
    public static String getLogoImageUrl(BaseItemDto item, ApiClient apiClient) {
        if (item != null) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(400);
            options.setImageType(ImageType.Logo);
            if (item.getHasLogo()) {
                options.setTag(item.getImageTags().get(ImageType.Logo));
                return apiClient.GetImageUrl(item, options);
            } else if (item.getParentLogoImageTag() != null) {
                options.setTag(item.getParentLogoImageTag());
                return apiClient.GetImageUrl(item.getParentLogoItemId(), options);
            }
        }

        return null;
    }

    public static String getBackdropImageUrl(BaseItemDto item, ApiClient apiClient, boolean random) {
        if (item.getBackdropCount() > 0) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(1200);
            options.setImageType(ImageType.Backdrop);
            int index = random ? randInt(0, item.getBackdropCount() - 1) : 0;
            options.setImageIndex(index);
            options.setTag(item.getBackdropImageTags().get(index));
            return apiClient.GetImageUrl(item, options);
        } else {
            if (item.getParentBackdropImageTags() != null && item.getParentBackdropImageTags().size() > 0) {
                ImageOptions options = new ImageOptions();
                options.setMaxWidth(1200);
                options.setImageType(ImageType.Backdrop);
                int index = random ? randInt(0, item.getParentBackdropImageTags().size() - 1) : 0;
                options.setImageIndex(index);
                options.setTag(item.getParentBackdropImageTags().get(index));
                return apiClient.GetImageUrl(item.getParentBackdropItemId(), options);

            }
        }
        return null;
    }

    public static void getItemsToPlay(final BaseItemDto mainItem, final Response<String[]> outerResponse) {
        final List<String> items = new ArrayList<>();
        final GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
        ItemQuery query = new ItemQuery();

        switch (mainItem.getType()) {
            case "Episode":
                items.add(serializer.SerializeToString(mainItem));
                //add subsequent episodes
                query.setParentId(mainItem.getSeasonId());
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setMinIndexNumber(mainItem.getIndexNumber() + 1);
                query.setSortBy(new String[] {ItemSortBy.SortName});
                query.setIncludeItemTypes(new String[]{"Episode"});
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.Path, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        for (BaseItemDto item : response.getItems()) {
                            if (item.getIndexNumber() > mainItem.getIndexNumber()) {
                                items.add(serializer.SerializeToString(item));
                            }
                        }
                        outerResponse.onResponse(items.toArray(new String[items.size()]));
                    }
                });
                break;
            case "Series":
            case "Season":
                //get all episodes
                query.setParentId(mainItem.getId());
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setIncludeItemTypes(new String[]{"Episode"});
                query.setSortBy(new String[]{ItemSortBy.SortName});
                query.setRecursive(true);
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.Path, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        for (BaseItemDto item : response.getItems()) {
                            items.add(serializer.SerializeToString(item));
                        }
                        outerResponse.onResponse(items.toArray(new String[items.size()]));
                    }
                });
                break;
            default:
                items.add(serializer.SerializeToString(mainItem));
                outerResponse.onResponse(items.toArray(new String[items.size()]));
                break;
        }
    }

    private static String divider = "   |   ";
    public static String getInfoRow(BaseItemDto item) {
        StringBuilder sb = new StringBuilder();
        if (item.getType().equals("Episode")) {
            sb.append(item.getParentIndexNumber());
            sb.append("x");
            sb.append(item.getIndexNumber());
        }

        if (item.getCommunityRating() != null) {
            addWithDivider(sb, item.getCommunityRating());
        }
        if (item.getCriticRating() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(item.getCriticRating());
            sb.append("%");
        }

        MediaStream video = null;

        if (item.getMediaStreams() != null) {
            for (MediaStream stream : item.getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Video) {
                    video = stream;
                    break;
                }
            }
        }

        if (video != null) {
            if (video.getWidth() > 1280) {
                addWithDivider(sb, "1080");
            } else if (video.getWidth() > 640) {
                addWithDivider(sb, "720");
            }
        }

        if (item.getRunTimeTicks() != null && item.getRunTimeTicks() >  0) {
            addWithDivider(sb, item.getRunTimeTicks() / 600000000);
            sb.append("m");
        }

        if (item.getOfficialRating() != null) {
            addWithDivider(sb, item.getOfficialRating());
        }

        if (item.getType().equals("Series")) {
            if (item.getAirDays() != null && item.getAirDays().size() > 0) {
                addWithDivider(sb, item.getAirDays().get(0));
                sb.append(" ");
            }
            if (item.getAirTime() != null) {
                sb.append(item.getAirTime());
            }
            if (item.getStatus() != null) {
                addWithDivider(sb, item.getStatus().toString());
            }
        } else {
            if (item.getPremiereDate() != null) {
                addWithDivider(sb, new SimpleDateFormat("d MMM y").format(item.getPremiereDate()));
            }
        }

        return sb.toString();
    }

    private static void addWithDivider(StringBuilder sb, String value) {
        if (sb.length() > 0) {
            sb.append(divider);
        }
        sb.append(value);
    }

    private static void addWithDivider(StringBuilder sb, Long value) {
        if (sb.length() > 0) {
            sb.append(divider);
        }
        sb.append(value);
    }

    private static void addWithDivider(StringBuilder sb, Float value) {
        if (sb.length() > 0) {
            sb.append(divider);
        }
        sb.append(value);
    }

    public static String GetFullName(BaseItemDto item) {
        switch (item.getType()) {
            case "Episode":
                return item.getSeriesName() + " " + item.getParentIndexNumber() + "x" + item.getIndexNumber() + " " + item.getName();
            default:
                return item.getName();
        }
    }

    public static List<MediaStream> GetSubtitleStreams(MediaSourceInfo mediaSource) {
        return GetStreams(mediaSource, MediaStreamType.Subtitle);
    }

    public static List<MediaStream> GetStreams(MediaSourceInfo mediaSource, MediaStreamType type) {
        ArrayList<MediaStream> streams = mediaSource.getMediaStreams();
        ArrayList<MediaStream> ret = new ArrayList<>();
        if (streams != null) {
            for (MediaStream stream : streams) {
                if (stream.getType() == type) {
                    ret.add(stream);
                }
            }
        }

        return ret;
    }

    public static MediaSourceInfo Play(BaseItemDto item, int position, VideoView view) {
        StreamBuilder builder = new StreamBuilder();
        Long mbPos = (long)position * 10000;
        ApiClient apiClient = TvApp.getApplication().getApiClient();
        MediaSourceInfo ret = null;

        if (item.getPath() != null && item.getPath().startsWith("http://")) {
            //try direct stream
            view.setVideoPath(item.getPath());
            TvApp.getApplication().getPlaybackController().setPlaybackMethod(PlayMethod.DirectStream);
            ret = item.getMediaSources().get(0);
        } else {
            VideoOptions options = new VideoOptions();
            options.setDeviceId(apiClient.getDeviceId());
            options.setItemId(item.getId());
            options.setMediaSources(item.getMediaSources());
            options.setMaxBitrate(30000000);

            options.setProfile(new AndroidProfile());
            StreamInfo info = builder.BuildVideoItem(options);
            view.setVideoPath(info.ToUrl(apiClient.getApiUrl()));
            TvApp.getApplication().getPlaybackController().setPlaybackMethod(info.getPlayMethod());
            ret = info.getMediaSource();
        }

        if (position > 0) {
            TvApp.getApplication().getPlaybackController().seek(position);
        }
        view.start();
        TvApp.getApplication().setCurrentPlayingItem(item);

        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId());
        startInfo.setPositionTicks(mbPos);
        apiClient.ReportPlaybackStartAsync(startInfo, new EmptyResponse());

        return ret;

    }

    public static void Stop(BaseItemDto item, long pos) {
        if (item != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            apiClient.ReportPlaybackStoppedAsync(info, new EmptyResponse());
            apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), new EmptyResponse());

            // now update the position info on the item itself so we can resume immediately
            UserItemDataDto userData = item.getUserData();
            if (userData == null) {
                userData = new UserItemDataDto();
                item.setUserData(userData);
            }

            userData.setLastPlayedDate(new Date());
            userData.setPlaybackPositionTicks(pos);
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
            info.setPlayMethod(TvApp.getApplication().getPlaybackController().getPlaybackMethod());
            apiClient.ReportPlaybackProgressAsync(info, new EmptyResponse());

        }

    }
    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    private static Random rand = new Random();
    public static int randInt(int min, int max) {
        if (max <= min) return min;

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}
