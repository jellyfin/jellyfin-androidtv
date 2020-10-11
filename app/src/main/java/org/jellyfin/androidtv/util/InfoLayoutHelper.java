package org.jellyfin.androidtv.util;

import android.app.Activity;
import android.graphics.Color;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.androidtv.util.apiclient.StreamHelper;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.SeriesStatus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class InfoLayoutHelper {

    private static int textSize = 16;
    private static int BTMARGIN = Utils.convertDpToPixel(TvApp.getApplication(), -2);

    public static void addInfoRow(Activity activity, BaseRowItem item, LinearLayout layout, boolean includeRuntime, boolean includeEndtime) {
        switch (item.getItemType()) {

            case BaseItem:
                addInfoRow(activity, item.getBaseItem(), layout, includeRuntime, includeEndtime);
                break;
            default:
                addSubText(activity, item, layout);
                break;
        }
    }
    public static void addInfoRow(Activity activity, BaseItemDto item, LinearLayout layout, boolean includeRuntime, boolean includeEndTime) {
        layout.removeAllViews();
        if (item.getId() != null) {
            addInfoRow(activity, item, layout, includeRuntime, includeEndTime, StreamHelper.getFirstAudioStream(item));
        }else{
            addProgramChannel(activity, item, layout);
        }
    }

    public static void addInfoRow(Activity activity, BaseItemDto item, LinearLayout layout, boolean includeRuntime, boolean includeEndTime, MediaStream audioStream) {
        addCriticInfo(activity, item, layout);
        switch (item.getBaseItemType()) {
            case Episode:
                addSeasonEpisode(activity, item, layout);
                addDate(activity, item, layout);
                break;
            case BoxSet:
                addBoxSetCounts(activity, item, layout);
                break;
            case Series:
                //addSeasonCount(activity, item, layout);
                addSeriesAirs(activity, item, layout);
                addDate(activity, item, layout);
                includeEndTime = false;
                break;
            case Program:
                addProgramInfo(activity, item, layout);
                break;
            case RecordingGroup:
                addRecordingCount(activity, item, layout);
                break;
            case MusicArtist:
                Integer artistAlbums = item.getAlbumCount() != null ? item.getAlbumCount() : item.getChildCount();
                addCount(activity, artistAlbums, layout, artistAlbums != null && artistAlbums == 1 ? activity.getResources().getString(R.string.lbl_album) : activity.getResources().getString(R.string.lbl_albums));
                return;
            case MusicAlbum:
                String artist = item.getAlbumArtist() != null ? item.getAlbumArtist() : item.getArtists() != null && item.getAlbumArtists().size() > 0 ? item.getArtists().get(0) : null;
                if (artist != null) {
                    addText(activity, artist+" ", layout, 500);
                }
                addDate(activity, item, layout);
                Integer songCount = item.getSongCount() != null ? item.getSongCount() : item.getChildCount();
                addCount(activity, songCount, layout, songCount == 1 ? activity.getResources().getString(R.string.lbl_song) : activity.getResources().getString(R.string.lbl_songs));
                return;
            case Playlist:
                if (item.getChildCount() != null) addCount(activity, item.getChildCount(), layout, item.getChildCount() == 1 ? activity.getResources().getString(R.string.lbl_item) : activity.getResources().getString(R.string.lbl_items));
                if (item.getCumulativeRunTimeTicks() != null) addText(activity, " ("+ TimeUtils.formatMillis(item.getCumulativeRunTimeTicks() / 10000)+")", layout, 300);
                break;
            default:
                addDate(activity, item, layout);

        }
        if (includeRuntime) addRuntime(activity, item, layout, includeEndTime);
        addSeriesStatus(activity, item, layout);
        addRatingAndRes(activity, item, layout);
        addMediaDetails(activity, audioStream, layout);
    }

    private static void addText(Activity activity, String text, LinearLayout layout, int maxWidth) {
        TextView textView = new TextView(activity);
        textView.setTextSize(textSize);
        textView.setMaxWidth(maxWidth);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(text + "  ");
        layout.addView(textView);

    }

    private static void addBoxSetCounts(Activity activity, BaseItemDto item, LinearLayout layout) {
        boolean hasSpecificCounts = false;
        if (item.getMovieCount() != null && item.getMovieCount() > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getMovieCount().toString()+" "+activity.getResources().getString(R.string.lbl_movies)+"  ");
            layout.addView(amt);
            hasSpecificCounts = true;

        }
        if (item.getSeriesCount() != null && item.getSeriesCount() > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getSeriesCount().toString()+" "+activity.getResources().getString(R.string.lbl_tv_series)+"  ");
            layout.addView(amt);
            hasSpecificCounts = true;
        }
        if (!hasSpecificCounts && item.getChildCount() != null && item.getChildCount() > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getChildCount().toString()+" "+ activity.getResources().getString(item.getChildCount() > 1 ? R.string.lbl_items : R.string.lbl_item) +"  ");
            layout.addView(amt);

        }
    }

    private static void addCount(Activity activity, Integer count, LinearLayout layout, String label) {
        if (count != null && count > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(count.toString()+" "+ label +"  ");
            layout.addView(amt);
        }
    }

    private static void addRecordingCount(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getRecordingCount() != null && item.getRecordingCount() > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getRecordingCount().toString() + " " + activity.getResources().getString(item.getRecordingCount() > 1 ? R.string.lbl_recordings : R.string.lbl_recording) + "  ");
            layout.addView(amt);
        }
    }

    private static void addSeriesAirs(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
            TextView textView = new TextView(activity);
            textView.setTextSize(textSize);
            textView.setText(item.getAirDays().get(0) + " " + Utils.getSafeValue(item.getAirTime(), "") +  "  ");
            layout.addView(textView);

        }
    }

    private static void addProgramChannel(Activity activity, BaseItemDto item, LinearLayout layout){
        TextView name = new TextView(activity);
        name.setTextSize(textSize);
        name.setText(BaseItemUtils.getProgramUnknownChannelName(item));
        layout.addView(name);
    }

    private static void addProgramInfo(Activity activity, BaseItemDto item, LinearLayout layout) {
        TextView name = new TextView(activity);
        name.setTextSize(textSize);
        name.setText(BaseItemUtils.getProgramSubText(item)+"  ");
        layout.addView(name);

        if (BaseItemUtils.isNew(item)) {
            addBlockText(activity, layout, TvApp.getApplication().getString(R.string.lbl_new), 12, Color.GRAY, R.drawable.dark_green_gradient);
            addSpacer(activity, layout, "  ");
        } else if (Utils.isTrue(item.getIsSeries()) && !Utils.isTrue(item.getIsNews())) {
            addBlockText(activity, layout, TvApp.getApplication().getString(R.string.lbl_repeat), 12, Color.GRAY, R.color.lb_default_brand_color);
            addSpacer(activity, layout, "  ");
        }
        if (Utils.isTrue(item.getIsLive())) {
            addBlockText(activity, layout, TvApp.getApplication().getString(R.string.lbl_live), 12, Color.GRAY, R.color.lb_default_brand_color);
            addSpacer(activity, layout, "  ");

        }
    }

    private static void addSubText(Activity activity, BaseRowItem item, LinearLayout layout) {
        layout.removeAllViews();
        TextView text = new TextView(activity);
        text.setTextSize(textSize);
        text.setText(item.getSubText() + " ");
        layout.addView(text);

    }

    private static void addRuntime(Activity activity, BaseItemDto item, LinearLayout layout, boolean includeEndtime) {
        Long runtime = Utils.getSafeValue(item.getRunTimeTicks(), item.getOriginalRunTimeTicks());
        if (runtime != null && runtime > 0) {
            long endTime = includeEndtime ? System.currentTimeMillis() + runtime / 10000 - (item.getUserData() != null && item.getCanResume() ? item.getUserData().getPlaybackPositionTicks()/10000 : 0) : 0;
            String text = runtime / 600000000 + activity.getString(R.string.lbl_min) + (endTime > 0 ? " (" + activity.getResources().getString(R.string.lbl_ends) + " " + android.text.format.DateFormat.getTimeFormat(activity).format(new Date(endTime)) + ")  " : "  ");
            TextView time = new TextView(activity);
            time.setTextSize(textSize);
            time.setText(text);
            layout.addView(time);
        }
    }

    private static void addSeasonEpisode(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getIndexNumber() != null) {
            String text = (item.getParentIndexNumber() != null ? "S"+item.getParentIndexNumber() : "") +" E"+item.getIndexNumber() + (item.getIndexNumberEnd() != null ? "-" + item.getIndexNumberEnd() : "")+"  ";
            TextView time = new TextView(activity);
            time.setTextSize(textSize);
            time.setText(text);
            layout.addView(time);
        }
    }

    private static void addCriticInfo(Activity activity, BaseItemDto item, LinearLayout layout) {
        int imagesize = Utils.convertDpToPixel(activity,textSize+2);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imagesize,imagesize);
        imageParams.setMargins(0, 5, 10, 0);
        boolean hasSomething = false;
        if (item.getCommunityRating() != null) {
            ImageView star = new ImageView(activity);
            star.setImageResource(R.drawable.star);
            star.setLayoutParams(imageParams);
            layout.addView(star);

            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getCommunityRating().toString()+" ");
            layout.addView(amt);

            hasSomething = true;
        }

        if (item.getCriticRating() != null) {
            ImageView tomato = new ImageView(activity);
            tomato.setLayoutParams(imageParams);
            if (item.getCriticRating() > 59) {
                tomato.setImageResource(R.drawable.fresh);
            } else {
                tomato.setImageResource(R.drawable.rotten);
            }

            layout.addView(tomato);
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getCriticRating().toString() + "% ");
            layout.addView(amt);

            hasSomething = true;

        }

        if (hasSomething) addSpacer(activity, layout, "  ");
    }

    private static void addDate(Activity activity, BaseItemDto item, LinearLayout layout) {
        TextView date = new TextView(activity);
        date.setTextSize(textSize);
        switch (item.getBaseItemType()) {
            case Person:
                StringBuilder sb = new StringBuilder();
                if (item.getPremiereDate() != null) {
                    sb.append(TvApp.getApplication().getString(R.string.lbl_born));
                    sb.append(new SimpleDateFormat("d MMM y").format(TimeUtils.convertToLocalDate(item.getPremiereDate())));
                }
                if (item.getEndDate() != null) {
                    sb.append("  |  Died ");
                    sb.append(new SimpleDateFormat("d MMM y").format(item.getEndDate()));
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
                    date.setText(android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(item.getStartDate()))
                            + "-"+ android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(item.getEndDate())));
                    layout.addView(date);
                    addSpacer(activity, layout, "    ");
                }
                break;
            case Series:
                if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    date.setText(item.getProductionYear().toString());
                    layout.addView(date);
                    addSpacer(activity, layout, "  ");
                }
                break;
            default:
                if (item.getPremiereDate() != null) {
                    date.setText(new SimpleDateFormat("d MMM y").format(TimeUtils.convertToLocalDate(item.getPremiereDate())));
                    layout.addView(date);
                    addSpacer(activity, layout, "  ");
                } else if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    date.setText(item.getProductionYear().toString());
                    layout.addView(date);
                    addSpacer(activity, layout, "  ");
                }
                break;
        }

    }

    private static void addRatingAndRes(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getOfficialRating() != null && !item.getOfficialRating().equals("0")) {
            addBlockText(activity, layout, item.getOfficialRating());
            addSpacer(activity, layout, "  ");
        }
        if (item.getMediaStreams() != null && item.getMediaStreams().size() > 0 && item.getMediaStreams().get(0).getWidth() != null) {
            int width = item.getMediaStreams().get(0).getWidth();
            if (width > 2000) {
                addBlockText(activity, layout, "4K");
            }else if (width > 1910) {
                addBlockText(activity, layout, "1080");
            } else if (width > 1270) {
                addBlockText(activity, layout, "720");
            } else addBlockText(activity, layout, activity.getString(R.string.lbl_sd));

            addSpacer(activity, layout, "  ");
        }
        if (Utils.isTrue(item.getHasSubtitles())) {
            addBlockText(activity, layout, "CC");
            addSpacer(activity, layout, "  ");

        }
    }

    private static void addSeriesStatus(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getBaseItemType() == BaseItemType.Series && item.getSeriesStatus() != null) {
            boolean continuing = item.getSeriesStatus() == SeriesStatus.Continuing;
            String status = continuing ? activity.getString(R.string.lbl__continuing) : activity.getString(R.string.lbl_ended);
            addBlockText(activity, layout, status, textSize-4, Color.LTGRAY, continuing ? R.drawable.green_gradient : R.drawable.red_gradient);
            addSpacer(activity, layout, "  ");
        }
    }

    private static void addMediaDetails(Activity activity, MediaStream stream, LinearLayout layout) {

        if (stream != null) {
            if (stream.getCodec() != null && stream.getCodec().trim().length() > 0) {
                String codec = stream.getCodec().equals("dca") || stream.getCodec().equals("DCA") ? "DTS" : stream.getCodec().equals("ac3") || stream.getCodec().equals("AC3") ? "Dolby" : stream.getCodec().toUpperCase();
                addBlockText(activity, layout, codec);
                addSpacer(activity, layout, " ");
            }
            if (stream.getChannelLayout() != null && stream.getChannelLayout().trim().length() > 0) {
                addBlockText(activity, layout, stream.getChannelLayout().toUpperCase());
                addSpacer(activity, layout, "  ");
            }
        }
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text) {
        addBlockText(activity, layout, text, textSize-4);
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text, int size) {
        addBlockText(activity, layout, text, size, Color.BLACK, R.drawable.block_text_bg);
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text, int size, int textColor, int backgroundRes) {
        TextView view = new TextView(activity);
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setText(" " + text + " ");
        view.setBackgroundResource(backgroundRes);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        params.setMargins(0,BTMARGIN,0,0);
        view.setLayoutParams(params);
        layout.addView(view);

    }

    private static void addSpacer(Activity activity, LinearLayout layout, String sp) {
        addSpacer(activity, layout, sp, textSize);
    }

    public static void addSpacer(Activity activity, LinearLayout layout, String sp, int size) {
        TextView mSpacer = new TextView(activity);
        mSpacer.setTextSize(size);
        mSpacer.setText(sp);
        layout.addView(mSpacer);

    }

    public static void addSpacer(Activity activity, FlexboxLayout layout, String sp, int size) {
        TextView mSpacer = new TextView(activity);
        mSpacer.setTextSize(size);
        mSpacer.setText(sp);
        layout.addView(mSpacer);
    }
}
