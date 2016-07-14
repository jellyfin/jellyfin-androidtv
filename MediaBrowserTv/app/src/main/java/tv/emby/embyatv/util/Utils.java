package tv.emby.embyatv.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
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
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ConnectionResult;
import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.Response;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.profiles.AndroidProfileOptions;
import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dlna.StreamInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.StudioDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.library.PlayAccess;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.querying.ItemFields;
import mediabrowser.model.querying.ItemFilter;
import mediabrowser.model.querying.ItemQuery;
import mediabrowser.model.querying.ItemSortBy;
import mediabrowser.model.querying.ItemsResult;
import mediabrowser.model.querying.SimilarItemsQuery;
import mediabrowser.model.session.PlaybackProgressInfo;
import mediabrowser.model.session.PlaybackStartInfo;
import mediabrowser.model.session.PlaybackStopInfo;
import mediabrowser.model.users.AuthenticationResult;
import tv.emby.embyatv.BuildConfig;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.browsing.MainActivity;
import tv.emby.embyatv.details.FullDetailsActivity;
import tv.emby.embyatv.details.ItemListActivity;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.startup.ConnectActivity;
import tv.emby.embyatv.startup.DpadPwActivity;
import tv.emby.embyatv.startup.LogonCredentials;
import tv.emby.embyatv.startup.SelectServerActivity;
import tv.emby.embyatv.startup.SelectUserActivity;

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

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     *
     * @param millis
     * @return
     */
    public static String formatMillis(long millis) {
        String result = "";
        long hr = millis / 3600000;
        millis %= 3600000;
        long min = millis / 60000;
        millis %= 60000;
        long sec = millis / 1000;
        if (hr > 0) {
            result += hr + ":";
        }
        if (min >= 0) {
            if (min > 9) {
                result += min + ":";
            } else {
                result += (hr > 0 ? "0" : "") + min + ":";
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

    public static boolean isLiveTv(BaseItemDto item) {
        return "Program".equals(item.getType()) || "LiveTvChannel".equals(item.getType());
    }

    private static String[] ThumbFallbackTypes = new String[] {"Episode"};

    public static Double getImageAspectRatio(BaseItemDto item, boolean preferParentThumb) {
        if (preferParentThumb && (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null)) return 1.779;

        if (Arrays.asList(ThumbFallbackTypes).contains(item.getType())) {
            if (item.getPrimaryImageAspectRatio() != null) return item.getPrimaryImageAspectRatio();
            if (item.getParentThumbItemId() != null || item.getSeriesThumbImageTag() != null) return 1.779;
        }

        return item.getPrimaryImageAspectRatio() != null ? item.getPrimaryImageAspectRatio() : .77777;
    }

    public static String getPrimaryImageUrl(BaseItemPerson item, ApiClient apiClient, int maxHeight) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(maxHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetPersonImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(StudioDto studio, ApiClient apiClient, int maxHeight) {
        ImageOptions options = new ImageOptions();
        options.setTag(studio.getPrimaryImageTag());
        options.setMaxHeight(maxHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(studio.getId(), options);
    }

    public static String getPrimaryImageUrl(UserDto item, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setTag(item.getPrimaryImageTag());
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetUserImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, int width, int height) {
        if (!item.getHasPrimaryImage()) return null;
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxWidth(width);
        options.setMaxHeight(height);
        options.setImageType(ImageType.Primary);
        return TvApp.getApplication().getApiClient().GetImageUrl(item, options);

    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient) {
        if (!item.getHasPrimaryImage()) return null;
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(item, options);
    }

    public static String getPrimaryImageUrl(ChannelInfoDto item, ApiClient apiClient) {
        if (!item.getHasPrimaryImage()) return null;
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Primary));
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(ImageType.Primary);
        return apiClient.GetImageUrl(item, options);
    }

    private static String[] ProgressIndicatorTypes = new String[] {"Episode", "Movie", "MusicVideo", "Video"};

    public static String getImageUrl(String itemId, ImageType imageType, String imageTag, ApiClient apiClient) {
        ImageOptions options = new ImageOptions();
        options.setMaxHeight(maxPrimaryImageHeight);
        options.setImageType(imageType);
        options.setTag(imageTag);

        return apiClient.GetImageUrl(itemId, options);
    }

    public static String getBannerImageUrl(BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasBanner()) return getPrimaryImageUrl(item, apiClient, !"MusicArtist".equals(item.getType()) && !"MusicAlbum".equals(item.getType()), false, maxHeight);
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Banner));
        options.setImageType(ImageType.Banner);
        UserItemDataDto userData = item.getUserData();
        if (userData != null && !"MusicArtist".equals(item.getType()) && !"MusicAlbum".equals(item.getType())) {
            if (Arrays.asList(ProgressIndicatorTypes).contains(item.getType()) && userData.getPlayedPercentage() != null
                    && userData.getPlayedPercentage() > 0 && userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }

            options.setAddPlayedIndicator(userData.getPlayed());
            if (item.getIsFolder() && userData.getUnplayedItemCount() != null && userData.getUnplayedItemCount() > 0)
                options.setUnPlayedCount(userData.getUnplayedItemCount());

        }

        return apiClient.GetImageUrl(item.getId(), options);

    }

    public static String getThumbImageUrl(BaseItemDto item, ApiClient apiClient, int maxHeight) {
        if (!item.getHasThumb()) return getPrimaryImageUrl(item, apiClient, !"MusicArtist".equals(item.getType()) && !"MusicAlbum".equals(item.getType()), true, maxHeight);
        ImageOptions options = new ImageOptions();
        options.setTag(item.getImageTags().get(ImageType.Thumb));
        options.setImageType(ImageType.Thumb);
        UserItemDataDto userData = item.getUserData();
        if (userData != null && !"MusicArtist".equals(item.getType()) && !"MusicAlbum".equals(item.getType())) {
            if (Arrays.asList(ProgressIndicatorTypes).contains(item.getType()) && userData.getPlayedPercentage() != null
                    && userData.getPlayedPercentage() > 0 && userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }

            options.setAddPlayedIndicator(userData.getPlayed());
            if (item.getIsFolder() && userData.getUnplayedItemCount() != null && userData.getUnplayedItemCount() > 0)
                options.setUnPlayedCount(userData.getUnplayedItemCount());

        }

        return apiClient.GetImageUrl(item.getId(), options);

    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, Boolean showWatched, boolean preferParentThumb, int maxHeight) {
        return getPrimaryImageUrl(item, apiClient, showWatched, true, preferParentThumb, false, maxHeight);
    }

    public static String getPrimaryImageUrl(BaseItemDto item, ApiClient apiClient, Boolean showWatched, boolean showProgress, boolean preferParentThumb, boolean preferSeriesPoster, int maxHeight) {
        ImageOptions options = new ImageOptions();
        String itemId = item.getId();
        String imageTag = item.getImageTags() != null ? item.getImageTags().get(ImageType.Primary) : null;
        ImageType imageType = ImageType.Primary;
        if (preferSeriesPoster && item.getType().equals("Episode") && item.getSeasonId() != null) {
            imageTag = null;
            itemId = item.getSeasonId();
        } else if (preferSeriesPoster && item.getType().equals("Episode") && item.getSeriesPrimaryImageTag() != null && item.getSeriesId() != null) {
            imageTag = item.getSeriesPrimaryImageTag();
            itemId = item.getSeriesId();
        } else if (preferParentThumb || (item.getType().equals("Episode") && imageTag == null)) {
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
        if ("Audio".equals(item.getType()) && !item.getHasPrimaryImage()) {
            //Try the album or artist
            if (item.getAlbumId() != null && item.getAlbumPrimaryImageTag() != null) {
                imageTag = item.getAlbumPrimaryImageTag();
                itemId = item.getAlbumId();
                imageType = ImageType.Primary;
            } else if (item.getAlbumArtists() != null && item.getAlbumArtists().size() > 0) {
                itemId = item.getAlbumArtists().get(0).getId();
                imageTag = null;
            }
        }
        options.setMaxHeight(maxHeight);
        options.setImageType(imageType);
        UserItemDataDto userData = item.getUserData();
        if (userData != null) {
            if (showProgress && Arrays.asList(ProgressIndicatorTypes).contains(item.getType()) && userData.getPlayedPercentage() != null
                    && userData.getPlayedPercentage() > 0 && userData.getPlayedPercentage() < 99) {
                Double pct = userData.getPlayedPercentage();
                options.setPercentPlayed(pct.intValue());
            }
            if (showWatched) {
                options.setAddPlayedIndicator(userData.getPlayed());
                if (item.getIsFolder() && userData.getUnplayedItemCount() != null && userData.getUnplayedItemCount() > 0)
                    options.setUnPlayedCount(userData.getUnplayedItemCount());
            }

        }

        options.setTag(imageTag);

        return apiClient.GetImageUrl(itemId, options);
    }

    public static String getLogoImageUrl(BaseItemDto item, ApiClient apiClient) {
        if (item != null) {
            ImageOptions options = new ImageOptions();
            options.setMaxWidth(440);
            options.setImageType(ImageType.Logo);
            if (item.getHasLogo()) {
                options.setTag(item.getImageTags().get(ImageType.Logo));
                return apiClient.GetImageUrl(item, options);
            } else if (item.getParentLogoImageTag() != null) {
                options.setTag(item.getParentLogoImageTag());
                return apiClient.GetImageUrl(item.getParentLogoItemId(), options);
            } else if (item.getSeriesId() != null) {
                options.setTag(null);
                return apiClient.GetImageUrl(item.getSeriesId(), options);
            }
        }

        return null;
    }

    public static String getBackdropImageUrl(BaseItemDto item, ApiClient apiClient, boolean random) {
        if (item != null) {
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
        }

        return null;
    }

    public static void getItemsToPlay(final BaseItemDto mainItem, boolean allowIntros, final boolean shuffle, final Response<List<BaseItemDto>> outerResponse) {
        final List<BaseItemDto> items = new ArrayList<>();
        ItemQuery query = new ItemQuery();
        TvApp.getApplication().setPlayingIntros(false);

        switch (mainItem.getType()) {
            case "Episode":
                items.add(mainItem);
                if (TvApp.getApplication().getPrefs().getBoolean("pref_enable_tv_queuing", true)) {
                    MediaManager.setVideoQueueModified(false); // we are automatically creating new queue
                    //add subsequent episodes
                    if (mainItem.getSeasonId() != null && mainItem.getIndexNumber() != null) {
                        query.setParentId(mainItem.getSeasonId());
                        query.setIsVirtualUnaired(false);
                        query.setMinIndexNumber(mainItem.getIndexNumber() + 1);
                        query.setSortBy(new String[] {ItemSortBy.SortName});
                        query.setIncludeItemTypes(new String[]{"Episode"});
                        query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Path, ItemFields.Chapters, ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                        TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                            @Override
                            public void onResponse(ItemsResult response) {
                                if (response.getTotalRecordCount() > 0) {
                                    for (BaseItemDto item : response.getItems()) {
                                        if (item.getIndexNumber() > mainItem.getIndexNumber()) {
                                            if (!LocationType.Virtual.equals(item.getLocationType())) {
                                                items.add(item);

                                            } else {
                                                //stop adding when we hit a missing one
                                                break;
                                            }
                                        }
                                    }
                                }
                                outerResponse.onResponse(items);
                            }
                        });
                    } else {
                        TvApp.getApplication().getLogger().Info("Unable to add subsequent episodes due to lack of season or episode data.");
                        outerResponse.onResponse(items);
                    }
                } else {
                    outerResponse.onResponse(items);
                }
                break;
            case "Series":
            case "Season":
            case "BoxSet":
            case "Folder":
                //get all videos
                query.setParentId(mainItem.getId());
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setIncludeItemTypes(new String[]{"Episode", "Movie", "Video"});
                query.setSortBy(new String[]{shuffle ? ItemSortBy.Random : ItemSortBy.SortName});
                query.setRecursive(true);
                query.setLimit(50); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Chapters, ItemFields.Path, ItemFields.Overview, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        Collections.addAll(items, response.getItems());
                        outerResponse.onResponse(items);
                    }
                });
                break;
            case "MusicAlbum":
            case "MusicArtist":
                //get all songs
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                query.setIncludeItemTypes(new String[]{"Audio"});
                query.setSortBy(shuffle ? new String[] {ItemSortBy.Random} : "MusicArtist".equals(mainItem.getType()) ? new String[] {ItemSortBy.Album} : new String[] {ItemSortBy.SortName});
                query.setRecursive(true);
                query.setLimit(150); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                query.setParentId(mainItem.getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(Arrays.asList(response.getItems()));
                    }
                });
                break;
            case "Playlist":
                if (mainItem.getId().equals(ItemListActivity.FAV_SONGS)) {
                    query.setFilters(new ItemFilter[] {ItemFilter.IsFavoriteOrLikes});
                } else {
                    query.setParentId(mainItem.getId());
                }
                query.setIsMissing(false);
                query.setIsVirtualUnaired(false);
                if (shuffle) query.setSortBy(new String[] {ItemSortBy.Random});
                query.setRecursive(true);
                query.setLimit(150); // guard against too many items
                query.setFields(new ItemFields[] {ItemFields.MediaSources, ItemFields.MediaStreams, ItemFields.Chapters, ItemFields.Path, ItemFields.PrimaryImageAspectRatio});
                query.setUserId(TvApp.getApplication().getCurrentUser().getId());
                TvApp.getApplication().getApiClient().GetItemsAsync(query, new Response<ItemsResult>() {
                    @Override
                    public void onResponse(ItemsResult response) {
                        outerResponse.onResponse(Arrays.asList(response.getItems()));
                    }
                });
                break;

            case "Program":
                if (mainItem.getParentId() == null) {
                    outerResponse.onError(new Exception("No Channel ID"));
                    return;
                }

                //We retrieve the channel the program is on (which should be the program's parent)
                TvApp.getApplication().getApiClient().GetItemAsync(mainItem.getParentId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        // fill in info about the specific program for display
                        response.setPremiereDate(mainItem.getPremiereDate());
                        response.setEndDate(mainItem.getEndDate());
                        response.setOfficialRating(mainItem.getOfficialRating());
                        response.setRunTimeTicks(mainItem.getRunTimeTicks());
                        items.add(response);
                        outerResponse.onResponse(items);
                    }

                    @Override
                    public void onError(Exception exception) {
                        super.onError(exception);
                    }
                });
                break;

            case "TvChannel":
                // Retrieve full channel info for display
                TvApp.getApplication().getApiClient().GetLiveTvChannelAsync(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ChannelInfoDto>() {
                    @Override
                    public void onResponse(ChannelInfoDto response) {
                        // get current program info and fill it into our item
                        BaseItemDto program = response.getCurrentProgram();
                        if (program != null) {
                            mainItem.setPremiereDate(program.getStartDate());
                            mainItem.setEndDate(program.getEndDate());
                            mainItem.setOfficialRating(program.getOfficialRating());
                            mainItem.setRunTimeTicks(program.getRunTimeTicks());
                        }
                        addMainItem(mainItem, items, outerResponse);
                    }
                });
                break;

            default:
                if (allowIntros && TvApp.getApplication().getPrefs().getBoolean("pref_enable_cinema_mode", true)) {
                    //Intros
                    TvApp.getApplication().getApiClient().GetIntrosAsync(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
                        @Override
                        public void onResponse(ItemsResult response) {
                            if (response.getTotalRecordCount() > 0){
                                Collections.addAll(items, response.getItems());
                                TvApp.getApplication().getLogger().Info(response.getTotalRecordCount() + " intro items added for playback.");
                                TvApp.getApplication().setPlayingIntros(true);
                            } else {
                                TvApp.getApplication().setPlayingIntros(false);
                            }
                            //Finally, the main item including subsequent parts
                            addMainItem(mainItem, items, outerResponse);
                        }

                        @Override
                        public void onError(Exception exception) {
                            TvApp.getApplication().getLogger().ErrorException("Error retrieving intros", exception);
                            addMainItem(mainItem, items, outerResponse);
                        }
                    });

                } else {
                    addMainItem(mainItem, items, outerResponse);
                }
                break;
        }
    }

    public static void play(final BaseItemDto item, final int pos, final boolean shuffle, final Context activity) {
        Utils.getItemsToPlay(item, pos == 0 && item.getType().equals("Movie"), shuffle, new Response<List<BaseItemDto>>() {
            @Override
            public void onResponse(List<BaseItemDto> response) {
                switch (item.getType()) {
                    case "MusicAlbum":
                    case "MusicArtist":
                        MediaManager.playNow(response);
                        break;
                    case "Playlist":
                        if ("Audio".equals(item.getMediaType())) {
                            MediaManager.playNow(response);

                        } else {
                            Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                            MediaManager.setCurrentVideoQueue(response);
                            intent.putExtra("Position", pos);
                            if (!(activity instanceof Activity))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        }
                        break;
                    case "Audio":
                        if (response.size() > 0) {
                            MediaManager.playNow(response.get(0));
                        }
                        break;

                    default:
                        Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                        MediaManager.setCurrentVideoQueue(response);
                        intent.putExtra("Position", pos);
                        if (!(activity instanceof Activity))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                }
            }
        });

    }

    public static void retrieveAndPlay(String id, boolean shuffle, Context activity) {
        retrieveAndPlay(id, shuffle, null, activity);
    }

    public static void retrieveAndPlay(String id, final boolean shuffle, final Long position, final Context activity) {
        TvApp.getApplication().getApiClient().GetItemAsync(id, TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
            @Override
            public void onResponse(BaseItemDto response) {
                Long pos = position != null ? position / 10000 : response.getUserData() != null ? response.getUserData().getPlaybackPositionTicks() / 10000 : 0;
                play(response, pos.intValue(), shuffle, activity);
            }

            @Override
            public void onError(Exception exception) {
                TvApp.getApplication().getLogger().ErrorException("Error retrieving item for playback", exception);
                Utils.showToast(activity, R.string.msg_video_playback_error);
            }
        });

    }

    public static void playInstantMix(String seedId) {
        getInstantMixAsync(seedId, new Response<BaseItemDto[]>() {
            @Override
            public void onResponse(BaseItemDto[] response) {
                if (response.length > 0) {
                    MediaManager.playNow(Arrays.asList(response));
                } else {
                    showToast(TvApp.getApplication(), R.string.msg_no_playable_items);
                }
            }
        });
    }

    public static void getInstantMixAsync(String seedId, final Response<BaseItemDto[]> outerResponse) {
        SimilarItemsQuery query = new SimilarItemsQuery();
        query.setId(seedId);
        query.setUserId(TvApp.getApplication().getCurrentUser().getId());
        query.setFields(new ItemFields[] {ItemFields.PrimaryImageAspectRatio, ItemFields.Genres});
        TvApp.getApplication().getApiClient().GetInstantMixFromItem(query, new Response<ItemsResult>() {
            @Override
            public void onResponse(ItemsResult response) {
                outerResponse.onResponse(response.getItems());
            }

            @Override
            public void onError(Exception exception) {
                outerResponse.onError(exception);
            }
        });
    }

    public static String getStoreUrl() {
        return isFireTv() ? "http://www.amazon.com/Emby-for-Fire-TV/dp/B00VVJKTW8/ref=sr_1_2?s=mobile-apps&ie=UTF8&qid=1430569449&sr=1-2" : "https://play.google.com/store/apps/details?id=tv.emby.embyatv";
    }

    private static void addMainItem(BaseItemDto mainItem, final List<BaseItemDto> items, final Response<List<BaseItemDto>> outerResponse) {
        items.add(mainItem);
        if (mainItem.getPartCount() != null && mainItem.getPartCount() > 1) {
            // get additional parts
            TvApp.getApplication().getApiClient().GetAdditionalParts(mainItem.getId(), TvApp.getApplication().getCurrentUser().getId(), new Response<ItemsResult>() {
                @Override
                public void onResponse(ItemsResult response) {
                    Collections.addAll(items, response.getItems());
                    outerResponse.onResponse(items);
                }
            });
        } else {
            outerResponse.onResponse(items);
        }

    }

    public static boolean CanPlay(BaseItemDto item) {
        return item.getPlayAccess().equals(PlayAccess.Full)
                && ((item.getIsPlaceHolder() == null || !item.getIsPlaceHolder())
                && (!item.getType().equals("Episode") || !item.getLocationType().equals(LocationType.Virtual)))
                && (!item.getType().equals("Person"))
                && (!item.getIsFolder() || item.getChildCount() == null || item.getChildCount() > 0);
    }

    private static String divider = "   |   ";
    public static String getInfoRow(BaseItemDto item) {
        StringBuilder sb = new StringBuilder();
        String type = item.getType();

        switch (type) {
            case "Person":
                if (item.getPremiereDate() != null) {
                    sb.append(TvApp.getApplication().getString(R.string.lbl_born));
                    sb.append(new SimpleDateFormat("d MMM y").format(convertToLocalDate(item.getPremiereDate())));
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
                    sb.append("S");
                    sb.append(item.getParentIndexNumber());
                    sb.append(", E");
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
                    if (video.getWidth() > 1910) {
                        addWithDivider(sb, "1080");
                    } else if (video.getWidth() > 1270) {
                        addWithDivider(sb, "720");
                    }
                }

                if (item.getRunTimeTicks() != null && item.getRunTimeTicks() >  0) {
                    addWithDivider(sb, item.getRunTimeTicks() / 600000000);
                    sb.append("m");
                }

                if (item.getOfficialRating() != null && !item.getOfficialRating().equals("0")) {
                    addWithDivider(sb, item.getOfficialRating());
                }

                switch (item.getType()) {
                    case "Series":
                        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
                            addWithDivider(sb, item.getAirDays().get(0));
                            sb.append(" ");
                        }
                        if (item.getAirTime() != null) {
                            sb.append(item.getAirTime());
                        }
                        if (item.getStatus() != null) {
                            addWithDivider(sb, item.getStatus());
                        }

                        break;
                    case "Program":
                    case "TvChannel":
                        if (item.getPremiereDate() != null && item.getEndDate() != null) {
                            addWithDivider(sb, android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(convertToLocalDate(item.getPremiereDate()))
                            + "-"+ android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(convertToLocalDate(item.getEndDate())));
                        }
                        break;
                    default:
                        if (item.getPremiereDate() != null) {
                            addWithDivider(sb, new SimpleDateFormat("d MMM y").format(convertToLocalDate(item.getPremiereDate())));
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
                return item.getSeriesName() + " S" + item.getParentIndexNumber() + ", E" + item.getIndexNumber() + (item.getIndexNumberEnd() != null ? "-" + item.getIndexNumberEnd() : "");
            case "Audio":
            case "MusicAlbum":
                // we actually want the artist name if available
                return (item.getAlbumArtist() != null ? item.getAlbumArtist() + " - " : "") + item.getName();
            default:
                return item.getName();
        }
    }

    public static String GetSubName(BaseItemDto item) {
        switch (item.getType()) {
            case "Episode":
                String addendum = item.getLocationType().equals(LocationType.Virtual) && item.getPremiereDate() != null ? " (" +  getFriendlyDate(Utils.convertToLocalDate(item.getPremiereDate())) + ")" : "";
                return item.getName() + addendum;
            case "Season":
                return item.getChildCount() != null && item.getChildCount() > 0 ? item.getChildCount() + " " + TvApp.getApplication().getString(R.string.lbl_episodes) : "";
            case "MusicAlbum":
                return item.getChildCount() != null && item.getChildCount() > 0 ? item.getChildCount() + " " + TvApp.getApplication().getString(item.getChildCount() > 1 ? R.string.lbl_songs : R.string.lbl_song) : "";
            case "Audio":
                return item.getName();
            default:
                return item.getOfficialRating();
        }

    }

    public static String GetProgramSubText(BaseItemDto baseItem) {
        Calendar start = Calendar.getInstance();
        start.setTime(Utils.convertToLocalDate(baseItem.getStartDate()));
        int day = start.get(Calendar.DAY_OF_YEAR);
        return baseItem.getChannelName() + " - " + (baseItem.getEpisodeTitle() != null ? baseItem.getEpisodeTitle() : "") + " " +
                ((day != Calendar.getInstance().get(Calendar.DAY_OF_YEAR) ? getFriendlyDate(start.getTime()) + " " : "") +
                        android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(start.getTime()) + "-"
                        + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(baseItem.getEndDate())));

    }

    public static BaseItemPerson GetFirstPerson(BaseItemDto item, String type) {
        if (item.getPeople() == null || item.getPeople().length < 1) return null;

        for (BaseItemPerson person : item.getPeople()) {
            if (type.equals(person.getType())) return person;
        }
        return null;
    }

    public static List<ChapterItemInfo> buildChapterItems(BaseItemDto item) {
        List<ChapterItemInfo> chapters = new ArrayList<>();
        ImageOptions options = new ImageOptions();
        options.setImageType(ImageType.Chapter);
        int i = 0;
        for (ChapterInfoDto dto : item.getChapters()) {
            ChapterItemInfo chapter = new ChapterItemInfo();
            chapter.setItemId(item.getId());
            chapter.setName(dto.getName());
            chapter.setStartPositionTicks(dto.getStartPositionTicks());
            if (dto.getHasImage()) {
                options.setTag(dto.getImageTag());
                options.setImageIndex(i);
                chapter.setImagePath(TvApp.getApplication().getApiClient().GetImageUrl(item.getId(), options));
            }
            chapters.add(chapter);
            i++;
        }

        return chapters;

    }

    public static MediaStream GetMediaStream(MediaSourceInfo mediaSource, int index) {
        if (mediaSource.getMediaStreams() == null || mediaSource.getMediaStreams().size() == 0) return null;
        for (MediaStream stream : mediaSource.getMediaStreams()) {
            if (stream.getIndex() == index) return stream;
        }
        return null;
    }

    public static List<MediaStream> GetSubtitleStreams(MediaSourceInfo mediaSource) {
        return GetStreams(mediaSource, MediaStreamType.Subtitle);
    }

    public static List<MediaStream> GetAudioStreams(MediaSourceInfo mediaSource) {
        return GetStreams(mediaSource, MediaStreamType.Audio);
    }

    public static MediaStream GetFirstAudioStream(BaseItemDto item) {
        if (item.getMediaSources() == null || item.getMediaSources().size() < 1) return null;
        List<MediaStream> streams = GetAudioStreams(item.getMediaSources().get(0));
        if (streams == null || streams.size() < 1) return null;
        return streams.get(0);
    }

    public static List<MediaStream> GetVideoStreams(MediaSourceInfo mediaSource) {
        return GetStreams(mediaSource, MediaStreamType.Video);
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

    public static void ReportStopped(BaseItemDto item, StreamInfo streamInfo, long pos) {
        if (item != null && streamInfo != null) {
            PlaybackStopInfo info = new PlaybackStopInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(pos);
            TvApp.getApplication().getPlaybackManager().reportPlaybackStopped(info, streamInfo, apiClient.getServerInfo().getId(), TvApp.getApplication().getCurrentUser().getId(), false, apiClient, new EmptyResponse());

            TvApp.getApplication().setLastPlayback(Calendar.getInstance());
            switch (item.getType()) {
                case "Movie":
                    TvApp.getApplication().setLastMoviePlayback(Calendar.getInstance());
                    break;
                case "Episode":
                    TvApp.getApplication().setLastTvPlayback(Calendar.getInstance());
                    break;
            }

        }

    }

    public static void ReportStart(BaseItemDto item, long pos) {
        PlaybackStartInfo startInfo = new PlaybackStartInfo();
        startInfo.setItemId(item.getId());
        startInfo.setPositionTicks(pos);
        TvApp.getApplication().getPlaybackManager().reportPlaybackStart(startInfo, false, TvApp.getApplication().getApiClient(), new EmptyResponse());
        TvApp.getApplication().getLogger().Info("Playback of " + item.getName() + " started.");

    }

    public static void EnterManualServerAddress(final Activity activity) {
        final EditText address = new EditText(activity);
        address.setHint(activity.getString(R.string.lbl_ip_hint));
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lbl_enter_server_address))
                .setMessage(activity.getString(R.string.lbl_valid_server_address))
                .setView(address)
                .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String addressValue = address.getText().toString();
                TvApp.getApplication().getLogger().Debug("Entered address: " + addressValue);
                signInToServer(TvApp.getApplication().getConnectionManager(), addressValue.indexOf(":") < 0 ? addressValue + ":8096" : addressValue, activity);
            }
        }).show();

    }

    public static void EnterManualUser(final Activity activity) {
        final EditText userName = new EditText(activity);
        userName.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.lbl_enter_user_name))
                .setView(userName)
                .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String userValue = userName.getText().toString();
                TvApp.getApplication().getLogger().Debug("Entered user: " + userValue);
                final EditText userPw = new EditText(activity);
                userPw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.lbl_enter_user_pw))
                        .setView(userPw)
                        .setNegativeButton(activity.getString(R.string.lbl_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing.
                            }
                        }).setPositiveButton(activity.getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Utils.loginUser(userName.getText().toString(), userPw.getText().toString(), TvApp.getApplication().getLoginApiClient(), activity);
                    }
                }).show();
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

    public static void ReportProgress(BaseItemDto item, StreamInfo currentStreamInfo, long position, boolean isPaused) {
        if (item != null && currentStreamInfo != null) {
            PlaybackProgressInfo info = new PlaybackProgressInfo();
            ApiClient apiClient = TvApp.getApplication().getApiClient();
            info.setItemId(item.getId());
            info.setPositionTicks(position);
            info.setIsPaused(isPaused);
            info.setCanSeek(currentStreamInfo.getRunTimeTicks() != null && currentStreamInfo.getRunTimeTicks() > 0);
            info.setIsMuted(TvApp.getApplication().isAudioMuted());
            info.setPlayMethod(currentStreamInfo.getPlayMethod());
            if (TvApp.getApplication().getPlaybackController() != null && TvApp.getApplication().getPlaybackController().isPlaying()) {
                info.setAudioStreamIndex(TvApp.getApplication().getPlaybackController().getAudioStreamIndex());
                info.setSubtitleStreamIndex(TvApp.getApplication().getPlaybackController().getSubtitleStreamIndex());
            }
            TvApp.getApplication().getPlaybackManager().reportPlaybackProgress(info, currentStreamInfo, false, apiClient, new EmptyResponse());
        }

    }

    public static boolean isTrue(Boolean value) {
        return value != null && value;
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

    public static Date convertToUtcDate(Date localDate) {


        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        Date convertedDate = new Date( localDate.getTime() - timeZone.getRawOffset() );


        if ( timeZone.inDaylightTime(localDate) ) {
            Date dstDate = new Date( convertedDate.getTime() - timeZone.getDSTSavings() );


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

    public static void signInToServer(IConnectionManager connectionManager, final ServerInfo server, final Activity activity) {
        connectionManager.Connect(server, new Response<ConnectionResult>() {
            @Override
            public void onResponse(ConnectionResult serverResult) {
                switch (serverResult.getState()) {
                    case SignedIn:
                    case ServerSignIn:
                        //Set api client for login
                        TvApp.getApplication().setLoginApiClient(serverResult.getApiClient());
                        //Open user selection
                        Intent userIntent = new Intent(activity, SelectUserActivity.class);
                        userIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(userIntent);
                        break;
                    default:
                        TvApp.getApplication().getLogger().Error("Unexpected response " + serverResult.getState() + " trying to sign in to specific server " + server.getLocalAddress());
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
                        Utils.showToast(activity, activity.getString(R.string.msg_error_connecting_server));
                }
            }

            @Override
            public void onError(Exception exception) {
                reportError(activity, activity.getString(R.string.msg_error_connecting_server));
            }
        });
    }

    public static AndroidProfileOptions getProfileOptions() {
        AndroidProfileOptions options = new AndroidProfileOptions(Build.MODEL);
        options.SupportsHls = false;
        options.SupportsMkv = true;
//        options.SupportsAc3 = is60();
//        options.SupportsDts = is60();
        return options;
    }

    public static String getFriendlyDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Calendar now = Calendar.getInstance();
        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) return TvApp.getApplication().getString(R.string.lbl_today);
            if (cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)+1) return TvApp.getApplication().getString(R.string.lbl_tomorrow);
            if (cal.get(Calendar.DAY_OF_YEAR) < now.get(Calendar.DAY_OF_YEAR)+7 && cal.get(Calendar.DAY_OF_YEAR) > now.get(Calendar.DAY_OF_YEAR)) return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        }

        return android.text.format.DateFormat.getDateFormat(TvApp.getApplication()).format(date);
    }

    public static void reportError(final Context context, final String msg) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(msg)
                .setMessage(context.getString(R.string.lbl_report_msg_question))
                .setNegativeButton(context.getString(R.string.lbl_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToast(context, context.getString(R.string.msg_report_not_sent));
                    }
                }).setPositiveButton(context.getString(R.string.lbl_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    new LogReporter().sendReport("User", null);
                    showToast(context, context.getString(R.string.msg_report_sent));
            }
        }).show();
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity) {
        loginUser(userName, pw, apiClient, activity, null);
    }

    public static void loginUser(String userName, String pw, ApiClient apiClient, final Activity activity, final String directEntryItemId) {
        try {
            apiClient.AuthenticateUserAsync(userName, pw, new Response<AuthenticationResult>() {
                @Override
                public void onResponse(AuthenticationResult authenticationResult) {
                    TvApp application = TvApp.getApplication();
                    application.getLogger().Debug("Signed in as " + authenticationResult.getUser().getName());
                    application.setCurrentUser(authenticationResult.getUser());
                    if (directEntryItemId == null) {
                        Intent intent = new Intent(activity, MainActivity.class);
                        activity.startActivity(intent);
                    } else {
                        Intent intent = new Intent(activity, FullDetailsActivity.class);
                        intent.putExtra("ItemId", directEntryItemId);
                        activity.startActivity(intent);
                    }
                }

                @Override
                public void onError(Exception exception) {
                    super.onError(exception);
                    TvApp.getApplication().getLogger().ErrorException("Error logging in", exception);
                    Utils.showToast(activity, activity.getString(R.string.msg_invalid_id_pw));
                }
            });
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void SaveLoginCredentials(LogonCredentials creds, String fileName) throws IOException {
        TvApp app = TvApp.getApplication();
        OutputStream credsFile = app.openFileOutput(fileName, Context.MODE_PRIVATE);
        credsFile.write(app.getSerializer().SerializeToString(creds).getBytes());
        credsFile.close();
        app.setConfiguredAutoCredentials(creds);
    }

    public static LogonCredentials GetSavedLoginCredentials(String fileName){
        TvApp app = TvApp.getApplication();
        try {
            InputStream credsFile = app.openFileInput(fileName);
            String json = ReadStringFromFile(credsFile);
            credsFile.close();
            return (LogonCredentials) app.getSerializer().DeserializeFromString(json, LogonCredentials.class);
        } catch (IOException e) {
            // none saved
            return new LogonCredentials(new ServerInfo(), new UserDto());
        } catch (Exception e) {
            app.getLogger().ErrorException("Error interpreting saved login",e);
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
        return TvApp.getApplication().getString(R.string.lbl_version_colon) + BuildConfig.VERSION_NAME;
    }

    public static String SafeToUpper(String value) {
        if (value == null) return "";
        return value.toUpperCase();
    }

    public static String FirstToUpper(String value) {
        if (value == null || value.length() == 0) return "";
        return value.substring(0, 1).toUpperCase() + (value.length() > 1 ? value.substring(1) : "");
    }

    public static String NullCoalesce(String obj, String def) {
        if (obj == null) return def;
        return obj;
    }

    public static int NullCoalesce(Integer obj, int def) {
        if (obj == null) return def;
        return obj;
    }

    public static Long NullCoalesce(Long obj, Long def) {
        if (obj == null) return def;
        return obj;
    }

    public static Double NullCoalesce(Double obj, Double def) {
        if (obj == null) return def;
        return obj;
    }

    public static boolean IsEmpty(String value) {
        return value == null || value.equals("");
    }

    public static boolean versionGreaterThanOrEqual(String firstVersion, String secondVersion) {
        try {
            String[] firstVersionComponents = firstVersion.split("[.]");
            String[] secondVersionComponents = secondVersion.split("[.]");
            int firstLength = firstVersionComponents.length;
            int secondLength = secondVersionComponents.length;
            int firstMajor = firstLength > 0 ? Integer.parseInt(firstVersionComponents[0]) : 0;
            int secondMajor = secondLength > 0 ? Integer.parseInt(secondVersionComponents[0]) : 0;
            int firstMinor = firstLength > 1 ? Integer.parseInt(firstVersionComponents[1]) : 0;
            int secondMinor = secondLength > 1 ? Integer.parseInt(secondVersionComponents[1]) : 0;
            int firstBuild = firstLength > 2 ? Integer.parseInt(firstVersionComponents[2]) : 0;
            int secondBuild = secondLength > 0 ? Integer.parseInt(secondVersionComponents[2]) : 0;
            int firstRelease = firstLength > 3 ? Integer.parseInt(firstVersionComponents[3]) : 0;
            int secondRelease = secondLength > 3 ? Integer.parseInt(secondVersionComponents[3]) : 0;

            if (firstMajor < secondMajor) return false;
            if (firstMajor == secondMajor && firstMinor < secondMinor) return false;
            if (firstMajor == secondMajor && firstMinor == secondMinor && firstBuild < secondBuild) return false;
            if (firstMajor == secondMajor && firstMinor == secondMinor && firstBuild == secondBuild && firstRelease < secondRelease) return false;

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static int getReleaseVersion(String version) {
        String[] components = version.split("[.]");
        return components.length > 2 ? Integer.parseInt(components[2]) : 0;
    }

    public static String getCurrentFormattedTime() {
        String fullTime = android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(new Date());
        return android.text.format.DateFormat.is24HourFormat(TvApp.getApplication()) ?
                fullTime
                : TextUtils.substring(fullTime, 0, fullTime.length()-3 ); // strip off am/pm
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    public static PopupMenu createPopupMenu(Activity activity, View view, int gravity) {
        return new PopupMenu(activity, view, gravity);
    }

    public static boolean isFireTv() {
        return Build.MODEL.startsWith("AFT");
    }
    public static boolean isFireTvStick() { return Build.MODEL.equals("AFTM"); }

    public static boolean is1stGenFireTv() { return Build.MODEL.equals("AFTB"); }

    public static boolean isShield() { return Build.MODEL.equals("SHIELD Android TV"); }

    public static boolean isNexus() { return Build.MODEL.equals("Nexus Player"); }

    public static boolean is50() {
        return Build.VERSION.SDK_INT >= 21;
    }
    public static boolean isGreaterThan51() { return Build.VERSION.RELEASE.equals("5.1.1") || is60(); }

    public static boolean is60() {
        return Build.VERSION.SDK_INT >= 23;
    }
    public static boolean isGingerbreadOrLater() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean supportsAc3() {
        return true;
    }

    public static int getBrandColor() {
        return TvApp.getApplication().getPrefs().getInt("pref_sideline_color", isFireTv() ?
                TvApp.getApplication().getResources().getColor(R.color.fastlane_fire) :
                TvApp.getApplication().getResources().getColor(R.color.fastlane_background));
    }

    public static void processPasswordEntry(Activity activity, UserDto user) {
        processPasswordEntry(activity, user, null);
    }

    public static void processPasswordEntry(final Activity activity, final UserDto user, final String directItemId) {
        if (TvApp.getApplication().getPrefs().getBoolean("pref_alt_pw_entry", false)) {
            Intent pwIntent = new Intent(activity, DpadPwActivity.class);
            pwIntent.putExtra("User", TvApp.getApplication().getSerializer().SerializeToString(user));
            pwIntent.putExtra("ItemId", directItemId);
            pwIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            activity.startActivity(pwIntent);
        } else {
            TvApp.getApplication().getLogger().Debug("Requesting dialog...");
            final EditText password = new EditText(activity);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(activity)
                    .setTitle("Enter Password")
                    .setMessage("Please enter password for " + user.getName())
                    .setView(password)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String pw = password.getText().toString();
                            Utils.loginUser(user.getName(), pw, TvApp.getApplication().getLoginApiClient(), activity, directItemId);
                        }
                    }).show();
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = aData & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String MD5(String text)  {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            md5hash = md.digest();
            return convertToHex(md5hash);

        } catch (UnsupportedEncodingException e) {
            return UUID.randomUUID().toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }

    public static void handleConnectionResponse(final IConnectionManager connectionManager,  final Activity activity, ConnectionResult response) {
        ILogger logger = TvApp.getApplication().getLogger();
        switch (response.getState()) {
            case ConnectSignIn:
                logger.Debug("Sign in with connect...");
                Intent intent = new Intent(activity, ConnectActivity.class);
                activity.startActivity(intent);
                break;

            case Unavailable:
                logger.Debug("No server available...");
                Utils.showToast(activity, "No MB Servers available...");
                break;
            case ServerSignIn:
                logger.Debug("Sign in with server " + response.getServers().get(0).getName() + " total: " + response.getServers().size());
                Utils.signInToServer(connectionManager, response.getServers().get(0), activity);
                break;
            case SignedIn:
                ServerInfo serverInfo = response.getServers() != null && response.getServers().size() > 0 && response.getServers().get(0).getUserLinkType() != null ? response.getServers().get(0) : null;
                if (serverInfo != null) {
                    // go straight in for connect only
                    response.getApiClient().GetUserAsync(serverInfo.getUserId(), new Response<UserDto>() {
                        @Override
                        public void onResponse(UserDto response) {
                            TvApp.getApplication().setCurrentUser(response);
                            TvApp.getApplication().setConnectLogin(true);
                            Intent homeIntent = new Intent(activity, MainActivity.class);
                            activity.startActivity(homeIntent);
                        }
                    });

                } else {
                    logger.Debug("Ignoring saved connection manager sign in");
                    connectionManager.GetAvailableServers(new Response<ArrayList<ServerInfo>>(){
                        @Override
                        public void onResponse(ArrayList<ServerInfo> serverResponse) {
                            if (serverResponse.size() == 1) {
                                //Signed in before and have just one server so go directly to user screen
                                Utils.signInToServer(connectionManager, serverResponse.get(0), activity);
                            } else {
                                //More than one server so show selection
                                Intent serverIntent = new Intent(activity, SelectServerActivity.class);
                                GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                                List<String> payload = new ArrayList<>();
                                for (ServerInfo server : serverResponse) {
                                    payload.add(serializer.SerializeToString(server));
                                }
                                serverIntent.putExtra("Servers", payload.toArray(new String[payload.size()]));
                                serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                activity.startActivity(serverIntent);
                            }
                        }
                    });

                }
                break;
            case ServerSelection:
                logger.Debug("Select A server");
                connectionManager.GetAvailableServers(new Response<ArrayList<ServerInfo>>(){
                    @Override
                    public void onResponse(ArrayList<ServerInfo> serverResponse) {
                        Intent serverIntent = new Intent(activity, SelectServerActivity.class);
                        GsonJsonSerializer serializer = TvApp.getApplication().getSerializer();
                        List<String> payload = new ArrayList<>();
                        for (ServerInfo server : serverResponse) {
                            payload.add(serializer.SerializeToString(server));
                        }
                        serverIntent.putExtra("Servers", payload.toArray(new String[payload.size()]));
                        serverIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        activity.startActivity(serverIntent);
                    }
                });
                break;
        }

    }

    public static boolean downMixAudio() {
        AudioManager am = (AudioManager) TvApp.getApplication().getSystemService(Context.AUDIO_SERVICE);
        if (am.isBluetoothA2dpOn()) {
            TvApp.getApplication().getLogger().Info("Downmixing audio due to wired headset");
            return true;
        }

        return (isFireTv() && !is50()) || "1".equals(TvApp.getApplication().getPrefs().getString("pref_audio_option","0"));
    }

    /**
     * Returns darker version of specified <code>color</code>.
     */
    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }

}
