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
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.SeriesStatus;
import org.koin.java.KoinJavaComponent;

import java.util.Calendar;
import java.util.Date;

public class InfoLayoutHelper {

    private static int textSize = 16;

    public static void addInfoRow(Context context, BaseRowItem item, LinearLayout layout, boolean includeRuntime, boolean includeEndtime) {
        switch (item.getItemType()) {

            case BaseItem:
                addInfoRow(context, item.getBaseItem(), layout, includeRuntime, includeEndtime);
                break;
            default:
                addSubText(context, item, layout);
                break;
        }
    }

    public static void addInfoRow(Context context, BaseItemDto item, LinearLayout layout, boolean includeRuntime, boolean includeEndTime) {
        layout.removeAllViews();
        if (item.getId() != null) {
            addInfoRow(context, item, layout, includeRuntime, includeEndTime, StreamHelper.getFirstAudioStream(item));
        }else{
            addProgramChannel(context, item, layout);
        }
    }

    public static void addInfoRow(Context context, BaseItemDto item, LinearLayout layout, boolean includeRuntime, boolean includeEndTime, MediaStream audioStream) {
        RatingType ratingType = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getDefaultRatingType());
        if (ratingType != RatingType.RATING_HIDDEN) {
            addCriticInfo(context, item, layout);
        }
        switch (item.getBaseItemType()) {
            case Episode:
                addSeasonEpisode(context, item, layout);
                addDate(context, item, layout);
                break;
            case BoxSet:
                addBoxSetCounts(context, item, layout);
                break;
            case Series:
                //addSeasonCount(context, item, layout);
                addSeriesAirs(context, item, layout);
                addDate(context, item, layout);
                includeEndTime = false;
                break;
            case Program:
                addProgramInfo(context, item, layout);
                break;
            case RecordingGroup:
                addRecordingCount(context, item, layout);
                break;
            case MusicArtist:
                Integer artistAlbums = item.getAlbumCount() != null ? item.getAlbumCount() : item.getChildCount();
                addCount(context, artistAlbums, layout, artistAlbums != null && artistAlbums == 1 ? context.getResources().getString(R.string.lbl_album) : context.getResources().getString(R.string.lbl_albums));
                return;
            case MusicAlbum:
                String artist = item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getArtists() != null && item.getAlbumArtists().size() > 0 ? item.getArtists().get(0) : null;
                if (artist != null) {
                    addText(context, artist+" ", layout, 500);
                }
                addDate(context, item, layout);
                Integer songCount = item.getSongCount() != null ? item.getSongCount() : item.getChildCount();
                addCount(context, songCount, layout, songCount == 1 ? context.getResources().getString(R.string.lbl_song) : context.getResources().getString(R.string.lbl_songs));
                return;
            case Playlist:
                if (item.getChildCount() != null) addCount(context, item.getChildCount(), layout, item.getChildCount() == 1 ? context.getResources().getString(R.string.lbl_item) : context.getResources().getString(R.string.lbl_items));
                if (item.getCumulativeRunTimeTicks() != null) addText(context, " ("+ TimeUtils.formatMillis(item.getCumulativeRunTimeTicks() / 10000)+")", layout, 300);
                break;
            default:
                addDate(context, item, layout);

        }
        if (includeRuntime) addRuntime(context, item, layout, includeEndTime);
        addSeriesStatus(context, item, layout);
        addRatingAndRes(context, item, layout);
        addMediaDetails(context, audioStream, layout);
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
            amt.setText(item.getMovieCount().toString()+" "+context.getResources().getString(R.string.lbl_movies)+"  ");
            layout.addView(amt);
            hasSpecificCounts = true;

        }
        if (item.getSeriesCount() != null && item.getSeriesCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getSeriesCount().toString()+" "+context.getResources().getString(R.string.lbl_tv_series)+"  ");
            layout.addView(amt);
            hasSpecificCounts = true;
        }
        if (!hasSpecificCounts && item.getChildCount() != null && item.getChildCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getChildCount().toString()+" "+ context.getResources().getString(item.getChildCount() > 1 ? R.string.lbl_items : R.string.lbl_item) +"  ");
            layout.addView(amt);

        }
    }

    private static void addCount(Context context, Integer count, LinearLayout layout, String label) {
        if (count != null && count > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(count.toString()+" "+ label +"  ");
            layout.addView(amt);
        }
    }

    private static void addRecordingCount(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getRecordingCount() != null && item.getRecordingCount() > 0) {
            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getRecordingCount().toString() + " " + context.getResources().getString(item.getRecordingCount() > 1 ? R.string.lbl_recordings : R.string.lbl_recording) + "  ");
            layout.addView(amt);
        }
    }

    private static void addSeriesAirs(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
            TextView textView = new TextView(context);
            textView.setTextSize(textSize);
            textView.setText(item.getAirDays().get(0) + " " + Utils.getSafeValue(item.getAirTime(), "") +  "  ");
            layout.addView(textView);

        }
    }

    private static void addProgramChannel(Context context, BaseItemDto item, LinearLayout layout){
        TextView name = new TextView(context);
        name.setTextSize(textSize);
        name.setText(BaseItemUtils.getProgramUnknownChannelName(item));
        layout.addView(name);
    }

    private static void addProgramInfo(@NonNull Context context, BaseItemDto item, LinearLayout layout) {
        TextView name = new TextView(context);
        name.setTextSize(textSize);
        name.setText(BaseItemUtils.getProgramSubText(item, context)+"  ");
        layout.addView(name);

        if (BaseItemUtils.isNew(item)) {
            addBlockText(context, layout, context.getString(R.string.lbl_new), 12, Color.GRAY, R.drawable.dark_green_gradient);
            addSpacer(context, layout, "  ");
        } else if (Utils.isTrue(item.getIsSeries()) && !Utils.isTrue(item.getIsNews())) {
            addBlockText(context, layout, context.getString(R.string.lbl_repeat), 12, Color.GRAY, R.color.lb_default_brand_color);
            addSpacer(context, layout, "  ");
        }
        if (Utils.isTrue(item.getIsLive())) {
            addBlockText(context, layout, context.getString(R.string.lbl_live), 12, Color.GRAY, R.color.lb_default_brand_color);
            addSpacer(context, layout, "  ");

        }
    }

    private static void addSubText(Context context, BaseRowItem item, LinearLayout layout) {
        layout.removeAllViews();
        TextView text = new TextView(context);
        text.setTextSize(textSize);
        text.setText(item.getSubText(context) + " ");
        layout.addView(text);

    }

    private static void addRuntime(Context context, BaseItemDto item, LinearLayout layout, boolean includeEndtime) {
        ClockBehavior clockBehavior = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());
        if (clockBehavior != ClockBehavior.ALWAYS && clockBehavior != ClockBehavior.IN_MENUS) {
            includeEndtime = false;
        }
        Long runtime = Utils.getSafeValue(item.getRunTimeTicks(), item.getOriginalRunTimeTicks());
        if (runtime != null && runtime > 0) {
            long endTime = includeEndtime ? System.currentTimeMillis() + runtime / 10000 - (item.getUserData() != null && item.getCanResume() ? item.getUserData().getPlaybackPositionTicks()/10000 : 0) : 0;
            String text = (int) Math.ceil((double) runtime / 600000000) + context.getString(R.string.lbl_min) + (endTime > 0 ? " (" + context.getResources().getString(R.string.lbl_ends) + " " + android.text.format.DateFormat.getTimeFormat(context).format(new Date(endTime)) + ")  " : "  ");
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
        int imagesize = Utils.convertDpToPixel(context,textSize+2);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imagesize,imagesize);
        imageParams.setMargins(0, 5, 10, 0);
        boolean hasSomething = false;
        if (item.getCommunityRating() != null) {
            ImageView star = new ImageView(context);
            star.setImageResource(R.drawable.ic_star);
            star.setLayoutParams(imageParams);
            layout.addView(star);

            TextView amt = new TextView(context);
            amt.setTextSize(textSize);
            amt.setText(item.getCommunityRating().toString()+" ");
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
            amt.setText(item.getCriticRating().toString() + "% ");
            layout.addView(amt);

            hasSomething = true;

        }

        if (hasSomething) addSpacer(context, layout, "  ");
    }

    private static void addDate(@NonNull Context context, BaseItemDto item, LinearLayout layout) {
        TextView date = new TextView(context);
        date.setTextSize(textSize);
        switch (item.getBaseItemType()) {
            case Person:
                StringBuilder sb = new StringBuilder();
                if (item.getPremiereDate() != null) {
                    sb.append(context.getString(R.string.lbl_born));
                    sb.append(DateFormat.getMediumDateFormat(context).format(TimeUtils.convertToLocalDate(item.getPremiereDate())));
                }
                if (item.getEndDate() != null) {
                    sb.append("  |  Died ");
                    sb.append(DateFormat.getMediumDateFormat(context).format(TimeUtils.convertToLocalDate(item.getEndDate())));
                    sb.append(" (");
                    sb.append(TimeUtils.numYears(item.getPremiereDate(), item.getEndDate()));
                    sb.append(")");
                } else {
                    if (item.getPremiereDate() != null) {
                        sb.append(" (");
                        sb.append(TimeUtils.numYears(item.getPremiereDate(), Calendar.getInstance()));
                        sb.append(")");
                    }
                }
                date.setText(sb.toString());
                layout.addView(date);
                break;

            case Program:
            case TvChannel:
                if (item.getStartDate() != null && item.getEndDate() != null) {
                    date.setText(DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(item.getStartDate()))
                            + "-"+ DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(item.getEndDate())));
                    layout.addView(date);
                    addSpacer(context, layout, "    ");
                }
                break;
            case Series:
                if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    date.setText(item.getProductionYear().toString());
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                }
                break;
            default:
                if (item.getPremiereDate() != null) {
                    date.setText(DateFormat.getMediumDateFormat(context).format(TimeUtils.convertToLocalDate(item.getPremiereDate())));
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                } else if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    date.setText(item.getProductionYear().toString());
                    layout.addView(date);
                    addSpacer(context, layout, "  ");
                }
                break;
        }

    }

    private static void addRatingAndRes(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getOfficialRating() != null && !item.getOfficialRating().equals("0")) {
            addBlockText(context, layout, item.getOfficialRating());
            addSpacer(context, layout, "  ");
        }
        if (item.getMediaStreams() != null && item.getMediaStreams().size() > 0 && item.getMediaStreams().get(0).getWidth() != null && item.getMediaStreams().get(0).getHeight() != null) {
            int width = item.getMediaStreams().get(0).getWidth();
            int height = item.getMediaStreams().get(0).getHeight();
            if (width <= 960 && height <= 576) {
                addBlockText(context, layout, context.getString(R.string.lbl_sd));
            } else if (width <= 1280 && height <= 962) {
                addBlockText(context, layout, "720");
            } else if (width <= 1920 && height <= 1440) {
                addBlockText(context, layout, "1080");
            } else if (width <= 4096 && height <= 3072) {
                addBlockText(context, layout, "4K");
            } else {
                addBlockText(context, layout, "8K");
            }

            addSpacer(context, layout, "  ");
        }
        if (Utils.isTrue(item.getHasSubtitles())) {
            addBlockText(context, layout, "CC");
            addSpacer(context, layout, "  ");

        }
    }

    private static void addSeriesStatus(Context context, BaseItemDto item, LinearLayout layout) {
        if (item.getBaseItemType() == BaseItemType.Series && item.getSeriesStatus() != null) {
            boolean continuing = item.getSeriesStatus() == SeriesStatus.Continuing;
            String status = continuing ? context.getString(R.string.lbl__continuing) : context.getString(R.string.lbl_ended);
            addBlockText(context, layout, status, textSize-4, Color.LTGRAY, continuing ? R.drawable.green_gradient : R.drawable.red_gradient);
            addSpacer(context, layout, "  ");
        }
    }

    private static void addMediaDetails(Context context, MediaStream stream, LinearLayout layout) {

        if (stream != null) {
            if (stream.getCodec() != null && stream.getCodec().trim().length() > 0) {
                String codec = stream.getCodec().equals("dca") || stream.getCodec().equals("DCA") ? "DTS" : stream.getCodec().equals("ac3") || stream.getCodec().equals("AC3") ? "Dolby" : stream.getCodec().toUpperCase();
                addBlockText(context, layout, codec);
                addSpacer(context, layout, " ");
            }
            if (stream.getChannelLayout() != null && stream.getChannelLayout().trim().length() > 0) {
                addBlockText(context, layout, stream.getChannelLayout().toUpperCase());
                addSpacer(context, layout, "  ");
            }
        }
    }

    public static void addBlockText(Context context, LinearLayout layout, String text) {
        addBlockText(context, layout, text, textSize-4);
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
        params.setMargins(0,Utils.convertDpToPixel(context, -2),0,0);
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
