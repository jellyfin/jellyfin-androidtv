package tv.mediabrowser.mediabrowsertv.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.text.InputType;
import android.view.Display;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.session.PlaybackStopInfo;
import mediabrowser.model.users.AuthenticationResult;
import tv.mediabrowser.mediabrowsertv.BuildConfig;
import tv.mediabrowser.mediabrowsertv.browsing.MainActivity;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.startup.LogonCredentials;
import tv.mediabrowser.mediabrowsertv.startup.SelectUserActivity;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    private static int maxPrimaryImageHeight = 370;

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

    public static Drawable writeOnDrawable(Resources resources, int drawableId, String text){

        Bitmap bm = BitmapFactory.decodeResource(resources, drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, 0, bm.getHeight()/2, paint);

        return new BitmapDrawable(resources, bm);
    }

    private static String[] ThumbFallbackTypes = new String[] {"Episode"};

    public static Double getImageAspectRatio(BaseItemDto item) {
        if (Arrays.asList(ThumbFallbackTypes).contains(item.getType())) {
            if (item.getPrimaryImageAspectRatio() != null) return item.getPrimaryImageAspectRatio();
            if (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null) return 1.777777;
        }

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : .72222;
    }

    public static String getPrimaryImageUrl(BaseItemPerson item, ApiClient apiClient, int maxHeight) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(maxHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetPersonImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(UserDto item, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetUserImageUrl(item, options);
    }

    private static String[] ProgressIndicatorTypes = new String[] {"Episode", "Movie", "MusicVideo", "Video"};

    public static String getImageUrl(String itemId, ImageType imageType, String imageTag, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(imageType);
        options.setTag(imageTag);

        return apiClient.GetImageUrl(itemId, options);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, Boolean showWatched, boolean preferParentThumb, int maxHeight) {
        ImageOptions options = new ImageOptions();
        String itemId = item.getId();
        String imageTag = item.getImageTags().get(ImageType.Primary);
        ImageType imageType = ImageType.Primary;
        if (preferParentThumb || (item.getType().equals("Episode") && imageTag == null)) {
            //try for thumb of season or series
            if (item.getParentThumbImageTag() != null) {
                imageTag = item.getParentThumbImageTag();
                itemId = item.getParentThumbItemId();
                imageType = ImageType.Thumb;
            } else if (item.getSeriesThumbImageTag() != null) {
                imageTag = item.getSeriesThumbImageTag();
                itemId = item.getSeriesId();
                imageType = ImageType.Thumb;
            }
        } else {
            if (item.getType().equals("Season") && imageTag == null) {
                imageTag = item.getSeriesPrimaryImageTag();
                itemId = item.getSeriesId();
            }
        }
        options.setMaxHeight(maxHeight);
        options.setImageType(imageType);
        UserItemDataDto userData = item.getUserData();
        if (userData != null) {
            if (Arrays.asList(ProgressIndicatorTypes).contains(item.getType()) && userData.getPlayedPercentage() != null
                    && userData.getPlayedPercentage() > 0 && userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
            if (showWatched) {
                options.setAddPlayedIndicator(userData.getPlayed());
                //if (item.getIsFolder() && userData.getUnplayedItemCount() != null && userData.getUnplayedItemCount() > 0) options.setUnplayedCount(userData.getUnplayedItemCount());
            }

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
                if (mainItem.getSeasonId() != null && mainItem.getIndexNumber() != null) {
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
                } else {
                    TvApp.getApplication().getLogger().Info("Unable to add subsequent episodes due to lack of season or episode data.");
                    outerResponse.onResponse(items.toArray(new String[items.size()]));
                }
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
                if (mainItem.getPartCount() != null && mainItem.getPartCount() > 1) {
                    // get additional parts
                    TvApp.getApplication().getApiClient().GetAdditionalParts(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            for (BaseItemDto item : response.getItems()) {
                                items.add(serializer.SerializeToString(item));
                            }
                            outerResponse.onResponse(items.toArray(new String[items.size()]));
                        }
                    });
                } else {
                    outerResponse.onResponse(items.toArray(new String[items.size()]));
                }
                break;
        }
    }

    public static boolean CanPlay(BaseItemDto item) {
        return item.getPlayAccess().equals(PlayAccess.Full)
                && (item.getLocationType().equals(LocationType.FileSystem) || item.getLocationType().equals(LocationType.Remote));
    }

    private static String divider = "   |   ";
    public static String getInfoRow(BaseItemDto item) {
        StringBuilder sb = new StringBuilder();
        String type = item.getType();

        switch (type) {
            case "Person":
                if (item.getPremiereDate() != null) {
                    sb.append("Born ");
                    sb.append(new SimpleDateFormat("d MMM y").format(item.getPremiereDate()));
                }
                if (item.getEndDate() != null) {
                    addWithDivider(sb, "Died ");
                    sb.append(new SimpleDateFormat("d MMM y").format(item.getEndDate()));
                    sb.append(" (");
                    sb.append(numYears(item.getPremiereDate(), item.getEndDate()));
                    sb.append(")");
                } else {
                    if (item.getPremiereDate() != null) {
                        sb.append(" (");
                        sb.append(numYears(item.getPremiereDate(), Calendar.getInstance()));
                        sb.append(")");
                    }
                }
                break;
            default:
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

        }

        return sb.toString();
    }

    public static int numYears(Date start, Date end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(end);
        return numYears(calStart, calEnd);
    }

    public static int numYears(Date start, Calendar end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        return numYears(calStart, end);
    }

    public static int numYears(Calendar start, Calendar end) {
        int age = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (end.get(Calendar.MONTH) < start.get(Calendar.MONTH)) {
            age--;
        } else if (end.get(Calendar.MONTH) == start.get(Calendar.MONTH)
                && end.get(Calendar.DAY_OF_MONTH) < start.get(Calendar.DAY_OF_MONTH)) {
            age--;
        }
        return age;
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

    public static List<MediaStream> GetAudioStreams(MediaSourceInfo mediaSource) {
        return GetStreams(mediaSource, MediaStreamType.Audio);
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

    public static void ReportStopped(BaseItemDto item, long pos) {
        if (item != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            apiClient.ReportPlaybackStoppedAsync(info, new EmptyResponse());
            apiClient.StopTranscodingProcesses(apiClient.getDeviceId(), new EmptyResponse());

        }

    }

    public static void EnterManualServerAddress(final Activity activity) {
        final EditText address = new EditText(activity);
        address.setHint("IP Address or full domain name");
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(activity)
                .setTitle("Enter Server Address")
                .setMessage("Please enter a valid server address")
                .setView(address)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String addressValue = address.getText().toString();
                TvApp.getApplication().getLogger().Debug("Entered address: " + addressValue);
                signInToServer(TvApp.getApplication().getConnectionManager(), addressValue + ":8096", activity);
            }
        }).show();

    }

    // send the tone to the "alarm" stream (classic beeps go there) with 50% volume
    private static ToneGenerator ToneHandler = new ToneGenerator(AudioManager.STREAM_ALARM, 50);

    public static void Beep() {
        MakeTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    public static void Beep(int ms) {
        MakeTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ms);
    }

    public static void ClickSound() {
        MakeTone(ToneGenerator.TONE_CDMA_PIP, 50);
    }

    public static void MakeTone(int type, int ms) {
        ToneHandler.startTone(type, ms);
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

    public static Date convertToLocalDate(Date utcDate) {


        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        Date convertedDate = new Date( utcDate.getTime() + timeZone.getRawOffset() );


        if ( timeZone.inDaylightTime(convertedDate) ) {
            Date dstDate = new Date( convertedDate.getTime() + timeZone.getDSTSavings() );


            if (timeZone.inDaylightTime( dstDate )) {
                convertedDate = dstDate;
            }
        }


        return convertedDate;
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

        return rand.nextInt((max - min) + 1) + min;
    }

    public static void signInToServer(IConnectionManager connectionManager, ServerInfo server, final Activity activity) {
        connectionManager.Connect(server, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                switch (serverResult.getState()) {
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(serverResult.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                }
            }


        });
    }

    public static void signInToServer(IConnectionManager connectionManager, String address, final Activity activity) {
        connectionManager.Connect(address, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                switch (serverResult.getState()) {
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(serverResult.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    default:
                        TvApp.getApplication().getLogger().Error("Unexpected response from server login "+ serverResult.getState());
                        reportError(activity, "Error Connecting to Server");
                }
            }

            @Override
            public void onError(Exception exception) {
                reportError(activity, "Error Connecting to Server");
            }
        });
    }

    public static void reportError(final Context context, final String msg) {
        new AlertDialog.Builder(context)
                .setTitle(msg)
                .setMessage("Would you like to send a report to the developer?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToast(context, "Report NOT sent");
                    }
                }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PutCustomAcraData();
                ACRA.getErrorReporter().handleException(new Exception(msg), false);
                showToast(context, "Report sent to developer. Thank you.");
            }
        }).show();
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity) {
        try {
            apiClient.AuthenticateUserAsync(userName, pw, new Response<AuthenticationResult>() {
                @Override
                public void onResponse(AuthenticationResult authenticationResult) {
                    TvApp application = TvApp.getApplication();
                    application.getLogger().Debug("Signed in as " + authenticationResult.getUser().getName());
                    application.setCurrentUser(authenticationResult.getUser());
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                }

                @Override
                public void onError(Exception exception) {
                    super.onError(exception);
                    TvApp.getApplication().getLogger().ErrorException("Error logging in", exception);
                    Utils.showToast(activity, "Invalid User id or password");
                }
            });
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void SaveLoginCredentials(LogonCredentials creds) throws IOException {
        TvApp app = TvApp.getApplication();
        OutputStream credsFile = app.openFileOutput("tv.mediabrowser.login.json", Context.MODE_PRIVATE);
        credsFile.write(app.getSerializer().SerializeToString(creds).getBytes());
        credsFile.close();
        app.setConfiguredAutoCredentials(creds);
    }

    public static LogonCredentials GetSavedLoginCredentials(){
        TvApp app = TvApp.getApplication();
        try {
            InputStream credsFile = app.openFileInput("tv.mediabrowser.login.json");
            String json = ReadStringFromFile(credsFile);
            credsFile.close();
            return (LogonCredentials) app.getSerializer().DeserializeFromString(json, LogonCredentials.class);
        } catch (IOException e) {
            // none saved
            return new LogonCredentials(new ServerInfo(), new UserDto());
        }
    }

    public static String ReadStringFromFile(InputStream inputStream) throws IOException {
        StringBuffer fileContent = new StringBuffer("");

        byte[] buffer = new byte[1024];
        int n;
        while ((n = inputStream.read(buffer)) != -1)
        {
            fileContent.append(new String(buffer, 0, n));
        }

        return fileContent.toString();
    }

    public static String VersionString() {
        return "Version: " + BuildConfig.VERSION_NAME;
    }

    public static String SafeToUpper(String value) {
        if (value == null) return "";
        return value.toUpperCase();
    }

    public static int NullCoalesce(Integer obj, int def) {
        if (obj == null) return def;
        return obj;
    }

    public static boolean IsEmpty(String value) {
        return value == null || value.equals("");
    }

    public static void PutCustomAcraData() {
        TvApp app = TvApp.getApplication();
        ApiClient apiClient = app.getApiClient();
        if (app != null && apiClient != null) {
            if (app.getCurrentUser() != null) ACRA.getErrorReporter().putCustomData("mbUser", app.getCurrentUser().getName());
            ACRA.getErrorReporter().putCustomData("serverInfo", app.getSerializer().SerializeToString(apiClient.getServerInfo()));
        }
    }

}
