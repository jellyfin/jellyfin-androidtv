package org.jellyfin.androidtv.util;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.preference.constant.RatingType;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.SeriesStatus;
import org.koin.java.KoinJavaComponent;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class InfoLayoutHelper {

    private static int textSize = 16;
    private static NumberFormat nf = NumberFormat.getInstance();
    private static NumberFormat nfp = NumberFormat.getPercentInstance();

    public static void addInfoRow(Context context, BaseRowItem item, LinearLayout layout, boolean includeRuntime, boolean includeEndtime) {
        switch (item.getBaseRowType()) {

            case BaseItem:
                addInfoRow(context, item.getBaseItem(), layout, includeRuntime, includeEndtime);
                break;
            default:
                addSubText(context, item, layout);
                break;
        }
    }

    public static void addInfoRow(Context context, BaseItemDto item, LinearLayout layout, boolean includeRuntime, boolean includeEndTime) {
        addInfoRow(context, item, 0, layout, includeRuntime, includeEndTime);
    }

    public static void addInfoRow(Context context, BaseItemDto item, int mediaSourceIndex, LinearLayout layout, boolean includeRuntime, boolean includeEndTime) {
        layout.removeAllViews();
        if (item.getId() != null) {
            RatingType ratingType = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDefaultRatingType());
            if (ratingType != RatingType.RATING_HIDDEN) {
                addCriticInfo(context, item, layout);
            }
            switch (item.getType()) {
                case EPISODE:
                    addSeasonEpisode(context, item, layout);
                    addDate(context, item, layout);
                    break;
                case BOX_SET:
                    addBoxSetCounts(context, item, layout);
                    break;
                case SERIES:
                    //addSeasonCount(context, item, layout);
                    addSeriesAirs(context, item, layout);
                    addDate(context, item, layout);
                    includeEndTime = false;
                    break;
                case PROGRAM:
                    addProgramInfo(context, item, layout);
                    break;
                case MUSIC_ARTIST:
                    Integer artistAlbums = item.getAlbumCount() != null ? item.getAlbumCount() : item.getChildCount();
                    addCount(context, artistAlbums, layout, artistAlbums != null && artistAlbums == 1 ? context.getResources().getString(R.string.lbl_album) : context.getResources().getString(R.string.lbl_albums));
                    return;
                case MUSIC_ALBUM:
                    String artist = item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getArtists() != null && item.getAlbumArtists().size() > 0 ? item.getArtists().get(0) : null;
                    if (artist != null) {
                        addText(context, artist + " ", layout, 500);
                    }
                    addDate(context, item, layout);
                    Integer songCount = item.getSongCount() != null ? item.getSongCount() : item.getChildCount();
                    addCount(context, songCount, layout, songCount == 1 ? context.getResources().getString(R.string.lbl_song) : context.getResources().getString(R.string.lbl_songs));
                    return;
                case PLAYLIST:
                    if (item.getChildCount() != null)
                        addCount(context, item.getChildCount(), layout, item.getChildCount() == 1 ? context.getResources().getString(R.string.lbl_item) : context.getResources().getString(R.string.lbl_items));
                    if (item.getCumulativeRunTimeTicks() != null)
                        addText(context, " (" + TimeUtils.formatMillis(item.getCumulativeRunTimeTicks() / 10000) + ")", layout, 300);
                    break;
                default:
                    addDate(context, item, layout);

            }
            if (includeRuntime) addRuntime(context, item, layout, includeEndTime);
            addSeriesStatus(context, item, layout);
            addRatingAndRes(context, item, mediaSourceIndex, layout);
            addMediaDetails(context, item, mediaSourceIndex, layout);

        } else {
            addProgramChannel(context, item, layout);
        }

    }

    private static void addText(Context context, String text, LinearLayout layout, int maxWidth) {
        TextView textView = new TextView(context);
        textView.setTextSize(textSize);
        textView.setMaxWidth(maxWidth);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(text + "  ");
        layout.addView(textView);

    }

    private static void addBoxSetCounts(Context context, BaseItemDto item, LinearLayout layout) {
        boolean hasSpecificCounts = false;
        if (item.getMovieCount() != null && item.getMovieCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getMovieCount().toString() + " " + context.getResources().getString(R.string.lbl_movies) + "  ");
            layout.addView(amt);
            hasSpecificCounts = true;

        }
        if (item.getSeriesCount() != null && item.getSeriesCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getSeriesCount().toString() + " " + context.getResources().getString(R.string.lbl_tv_series) + "  ");
            layout.addView(amt);
            hasSpecificCounts = true;
        }
        if (!hasSpecificCounts && item.getChildCount() != null && item.getChildCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getChildCount().toString() + " " + context.getResources().getString(item.getChildCount() > 1 ? R.string.lbl_items : R.string.lbl_item) + "  ");
            layout.addView(amt);

        }
    }

    private static void addCount(Context context, Integer count, LinearLayout layout, String label) {
        if (count != null && count > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(count.toString() + " " + label + "  ");
            layout.addView(amt);
        }
    }

    private static void addSeriesAirs(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
            TextView textView = new TextView(context);
            textView.setTextSize(textSize);
            textView.setText(item.getAirDays().get(0) + " " + Utils.getSafeValue(item.getAirTime(), "") + "  ");
            layout.addView(textView);

        }
    }

    private static void addProgramChannel(Context context, BaseItemDto item, LinearLayout layout) {
        TextView name = new TextView(context);
        name.setTextSize(textSize);
        name.setText(BaseItemExtensionsKt.getProgramUnknownChannelName(item));
        layout.addView(name);
    }

    private static void addProgramInfo(@NonNull Context context, BaseItemDto item, LinearLayout layout) {
        TextView name = new TextView(context);
        name.setTextSize(textSize);
        name.setText(BaseItemExtensionsKt.getProgramSubText(item, context) + "  ");
        layout.addView(name);

        if (BaseItemExtensionsKt.isNew(item)) {
            addBlockText(context, layout, context.getString(R.string.lbl_new), 12, Color.GRAY, R.drawable.dark_green_gradient);
            addSpacer(context, layout, "  ");
        } else if (Utils.isTrue(item.isSeries()) && !Utils.isTrue(item.isNews())) {
            addBlockText(context, layout, context.getString(R.string.lbl_repeat), 12, Color.GRAY, androidx.leanback.R.color.lb_default_brand_color);
            addSpacer(context, layout, "  ");
        }
        if (Utils.isTrue(item.isLive())) {
            addBlockText(context, layout, context.getString(R.string.lbl_live), 12, Color.GRAY, androidx.leanback.R.color.lb_default_brand_color);
            addSpacer(context, layout, "  ");

        }
    }

    private static void addSubText(Context context, BaseRowItem item, LinearLayout layout) {
        layout.removeAllViews();
        TextView text = new TextView(context);
        text.setTextSize(textSize);
        text.setText(Utils.getSafeValue(item.getSubText(context), "") + " ");
        layout.addView(text);

    }

    private static void addRuntime(Context context, BaseItemDto item, LinearLayout layout, boolean includeEndtime) {
        ClockBehavior clockBehavior = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());
        if (clockBehavior != ClockBehavior.ALWAYS && clockBehavior != ClockBehavior.IN_MENUS) {
            includeEndtime = false;
        }
        Long runtime = item.getRunTimeTicks();
        if (runtime != null && runtime > 0) {
            long endTime = includeEndtime ? System.currentTimeMillis() + runtime / 10000 - (item.getUserData() != null && item.getUserData().getPlaybackPositionTicks() > 0 ? item.getUserData().getPlaybackPositionTicks() / 10000 : 0) : 0;
            String text = nf.format((int) Math.ceil((double) runtime / 600000000)) + context.getString(R.string.lbl_min) + (endTime > 0 ? " (" + context.getResources().getString(R.string.lbl_ends) + " " + android.text.format.DateFormat.getTimeFormat(context).format(new Date(endTime)) + ")  " : "  ");
            TextView time = new TextView(context);
            time.setTextSize(textSize);
            time.setText(text);
            layout.addView(time);
        }
    }

    private static void addSeasonEpisode(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getIndexNumber() != null) {
            String text = (item.getParentIndexNumber() != null ? context.getString(R.string.lbl_season_number, item.getParentIndexNumber()) : "")
                    + (item.getIndexNumberEnd() != null && item.getIndexNumber() != null ? " " + context.getString(R.string.lbl_episode_range, item.getIndexNumber(), item.getIndexNumberEnd())
                    : item.getIndexNumber() != null ? " " + context.getString(R.string.lbl_episode_number, item.getIndexNumber()) : "")
                    + "  ";
            TextView time = new TextView(context);
            time.setTextSize(textSize);
            time.setText(text);
            layout.addView(time);
        }
    }

    private static void addCriticInfo(Context context, BaseItemDto item, LinearLayout layout) {
        int imagesize = Utils.convertDpToPixel(context, textSize + 2);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imagesize, imagesize);
        imageParams.setMargins(0, 5, 10, 0);
        boolean hasSomething = false;
        if (item.getCommunityRating() != null) {
            ImageView star = new ImageView(context);
            star.setImageResource(R.drawable.ic_star);
            star.setLayoutParams(imageParams);
            layout.addView(star);

            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(String.format(Locale.US, "%.1f ", item.getCommunityRating()));
            layout.addView(amt);

            hasSomething = true;
        }

        if (item.getCriticRating() != null) {
            ImageView tomato = new ImageView(context);
            tomato.setLayoutParams(imageParams);
            if (item.getCriticRating() > 59) {
                tomato.setImageResource(R.drawable.ic_rt_fresh);
            } else {
                tomato.setImageResource(R.drawable.ic_rt_rotten);
            }

            layout.addView(tomato);
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(nfp.format(item.getCriticRating() / 100) + " ");
            layout.addView(amt);

            hasSomething = true;

        }

        if (hasSomething) addSpacer(context, layout, "  ");
    }

    private static void addDate(@NonNull Context context, BaseItemDto item, LinearLayout layout) {
        TextView date = new TextView(context);
        date.setTextSize(textSize);
        switch (item.getType()) {
            case PERSON:
                StringBuilder sb = new StringBuilder();
                if (item.getPremiereDate() != null) {
                    sb.append(context.getString(R.string.lbl_born));
                    sb.append(DateFormat.getMediumDateFormat(context).format(TimeUtils.getDate(item.getPremiereDate())));
                }
                if (item.getEndDate() != null) {
                    sb.append("  |  Died ");
                    sb.append(DateFormat.getMediumDateFormat(context).format(TimeUtils.getDate(item.getEndDate())));
                    sb.append(" (");
                    sb.append(TimeUtils.numYears(TimeUtils.getDate(item.getPremiereDate()), TimeUtils.getDate(item.getEndDate())));
                    sb.append(")");
                } else {
                    if (item.getPremiereDate() != null) {
                        sb.append(" (");
                        sb.append(TimeUtils.numYears(TimeUtils.getDate(item.getPremiereDate()), Calendar.getInstance()));
                        sb.append(")");
                    }
                }
                date.setText(sb.toString());
                layout.addView(date);
                break;

            case PROGRAM:
            case TV_CHANNEL:
                if (item.getStartDate() != null && item.getEndDate() != null) {
                    date.setText(DateFormat.getTimeFormat(context).format(TimeUtils.getDate(item.getStartDate()))
                            + "-" + DateFormat.getTimeFormat(context).format(TimeUtils.getDate(item.getEndDate())));
                    layout.addView(date);
                    addSpacer(context, layout, "    ");
                }
                break;
            case SERIES:
                if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    nf.setGroupingUsed(false);
                    date.setText(nf.format(item.getProductionYear()));
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                }
                break;
            default:
                if (item.getPremiereDate() != null) {
                    date.setText(item.getPremiereDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                } else if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    nf.setGroupingUsed(false);
                    date.setText(nf.format(item.getProductionYear()));
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                }
                break;
        }

    }

    private static void addRatingAndRes(Context context, BaseItemDto item, int mediaSourceIndex, LinearLayout layout) {
        if (item.getOfficialRating() != null && !item.getOfficialRating().equals("0")) {
            addBlockText(context, layout, item.getOfficialRating());
            addSpacer(context, layout, "  ");
        }

        if (Utils.isTrue(item.getHasSubtitles())) {
            addBlockText(context, layout, "CC");
            addSpacer(context, layout, "  ");
        }

        MediaStream videoStream = StreamHelper.getFirstVideoStream(item, mediaSourceIndex);

        if (videoStream != null && videoStream.getWidth() != null && videoStream.getHeight() != null) {
            int width = videoStream.getWidth();
            int height = videoStream.getHeight();
            Boolean isInterlaced = videoStream.isInterlaced();
            if (width <= 960 && height <= 576) {
                addBlockText(context, layout, context.getString(R.string.lbl_sd));
            } else if (width <= 1280 && height <= 962) {
                addBlockText(context, layout, "720" + (isInterlaced == null || !isInterlaced ? "p" : "i"));
            } else if (width <= 1920 && height <= 1440) {
                addBlockText(context, layout, "1080" + (isInterlaced == null || !isInterlaced ? "p" : "i"));
            } else if (width <= 4096 && height <= 3072) {
                addBlockText(context, layout, "4K");
            } else {
                addBlockText(context, layout, "8K");
            }

            addSpacer(context, layout, " ");

            addVideoCodecDetails(context, layout, videoStream);
        }
    }

    private static void addSeriesStatus(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getType() == BaseItemKind.SERIES && item.getStatus() != null) {
            boolean continuing = item.getStatus().equalsIgnoreCase(SeriesStatus.CONTINUING.getSerialName());
            String status = continuing ? context.getString(R.string.lbl__continuing) : context.getString(R.string.lbl_ended);
            addBlockText(context, layout, status, textSize - 4, Color.LTGRAY, continuing ? R.drawable.green_gradient : R.drawable.red_gradient);
            addSpacer(context, layout, "  ");
        }
    }

    private static void addVideoCodecDetails(Context context, LinearLayout layout, MediaStream stream) {
        if (stream != null) {
            if (stream.getCodec() != null && stream.getCodec().trim().length() > 0) {
                String codec = stream.getCodec().toUpperCase();
                addBlockText(context, layout, codec);
                addSpacer(context, layout, "  ");
            }
            if (stream.getVideoDoViTitle() != null && stream.getVideoDoViTitle().trim().length() > 0) {
                addBlockText(context, layout, "VISION");
                addSpacer(context, layout, "  ");
            } else if (stream.getVideoRangeType() != null && !stream.getVideoRangeType().equals("SDR")) {
                addBlockText(context, layout, stream.getVideoRangeType().toUpperCase());
                addSpacer(context, layout, "  ");
            }
        }
    }

    private static void addMediaDetails(Context context, BaseItemDto item, int mediaSourceIndex, LinearLayout layout) {

        MediaStream audioStream = StreamHelper.getFirstAudioStream(item, mediaSourceIndex);

        if (audioStream != null) {
            if (audioStream.getProfile() != null && audioStream.getProfile().contains("Dolby Atmos")) {
                addBlockText(context, layout, "ATMOS");
                addSpacer(context, layout, " ");
            } else if (audioStream.getProfile() != null && audioStream.getProfile().contains("DTS:X")) {
                addBlockText(context, layout, "DTS:X");
                addSpacer(context, layout, " ");
            } else {
                String codec = null;
                if (audioStream.getProfile() != null && audioStream.getProfile().contains("DTS-HD")) {
                    codec = "DTS-HD";
                } else if (audioStream.getCodec() != null & audioStream.getCodec().trim().length() > 0) {
                    switch (audioStream.getCodec().toLowerCase()) {
                        case "dca":
                            codec = "DTS";
                            break;
                        case "eac3":
                            codec = "DD+";
                            break;
                        case "ac3":
                            codec = "DD";
                            break;
                        default:
                            codec = audioStream.getCodec().toUpperCase();
                    }
                }
                if (codec != null) {
                    addBlockText(context, layout, codec);
                    addSpacer(context, layout, " ");
                }
            }
            if (audioStream.getChannelLayout() != null && audioStream.getChannelLayout().trim().length() > 0) {
                addBlockText(context, layout, audioStream.getChannelLayout().toUpperCase());
                addSpacer(context, layout, "  ");
            }
        }
    }

    public static void addBlockText(Context context, LinearLayout layout, String text) {
        addBlockText(context, layout, text, textSize - 4);
    }

    public static void addBlockText(Context context, LinearLayout layout, String text, int size) {
        addBlockText(context, layout, text, size, Color.BLACK, R.drawable.block_text_bg);
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

    private static void addSpacer(Context context, LinearLayout layout, String sp) {
        addSpacer(context, layout, sp, textSize);
    }

    public static void addSpacer(Context context, LinearLayout layout, String sp, int size) {
        TextView mSpacer = new TextView(context);
        mSpacer.setTextSize(size);
        mSpacer.setText(sp);
        layout.addView(mSpacer);
    }
}
