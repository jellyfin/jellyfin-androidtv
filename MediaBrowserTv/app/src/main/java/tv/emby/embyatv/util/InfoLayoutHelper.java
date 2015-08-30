package tv.emby.embyatv.util;

import android.app.Activity;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.SeriesStatus;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.itemhandling.BaseRowItem;

/**
 * Created by Eric on 4/29/2015.
 */
public class InfoLayoutHelper {

    private static int textSize = 16;

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
        addCriticInfo(activity, item, layout);
        switch (item.getType()) {
            case "Episode":
                addSeasonEpisode(activity, item, layout);
                break;
            case "BoxSet":
                addBoxSetCounts(activity, item, layout);
                break;
            case "Series":
                addSeasonCount(activity, item, layout);
                addSeriesAirs(activity, item, layout);
                includeEndTime = false;
                break;
            case "Program":
                addChannelName(activity, item, layout);
        }
        addDate(activity, item, layout);
        if (includeRuntime) addRuntime(activity, item, layout, includeEndTime);
        addSeriesStatus(activity, item, layout);
        addRatingAndRes(activity, item, layout);
        addMediaDetails(activity, item, layout);
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

    private static void addSeasonCount(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getSeasonCount() != null && item.getSeasonCount() > 0) {
            TextView amt = new TextView(activity);
            amt.setTextSize(textSize);
            amt.setText(item.getSeasonCount().toString()+" "+ (item.getSeasonCount() == 1 ? activity.getResources().getString(R.string.lbl_season) : activity.getResources().getString(R.string.lbl_seasons)) +"  ");
            layout.addView(amt);

        }
    }

    private static void addSeriesAirs(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getAirDays() != null && item.getAirDays().size() > 0) {
            TextView textView = new TextView(activity);
            textView.setTextSize(textSize);
            textView.setText(item.getAirDays().get(0) + " " + Utils.NullCoalesce(item.getAirTime(), "") +  "  ");
            layout.addView(textView);

        }
    }

    private static void addChannelName(Activity activity, BaseItemDto item, LinearLayout layout) {
        if (item.getChannelName() != null) {
            TextView name = new TextView(activity);
            name.setTextSize(textSize);
            name.setText(item.getChannelName() + "  ");
            layout.addView(name);

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
        Long runtime = Utils.NullCoalesce(item.getRunTimeTicks(), item.getOriginalRunTimeTicks());
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
            String text = "S"+item.getParentIndexNumber()+" E"+item.getIndexNumber() + (item.getIndexNumberEnd() != null ? "-" + item.getIndexNumberEnd() : "")+"  ";
            TextView time = new TextView(activity);
            time.setTextSize(textSize);
            time.setText(text);
            layout.addView(time);
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
        switch (item.getType()) {
            case "Person":
                StringBuilder sb = new StringBuilder();
                if (item.getPremiereDate() != null) {
                    sb.append(TvApp.getApplication().getString(R.string.lbl_born));
                    sb.append(new SimpleDateFormat("d MMM y").format(Utils.convertToLocalDate(item.getPremiereDate())));
                }
                if (item.getEndDate() != null) {
                    sb.append("  |  Died ");
                    sb.append(new SimpleDateFormat("d MMM y").format(item.getEndDate()));
                    sb.append(" (");
                    sb.append(Utils.numYears(item.getPremiereDate(), item.getEndDate()));
                    sb.append(")");
                } else {
                    if (item.getPremiereDate() != null) {
                        sb.append(" (");
                        sb.append(Utils.numYears(item.getPremiereDate(), Calendar.getInstance()));
                        sb.append(")");
                    }
                }
                date.setText(sb.toString());
                layout.addView(date);
                break;

            case "Program":
            case "TvChannel":
                if (item.getPremiereDate() != null && item.getEndDate() != null) {
                    date.setText(android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(item.getPremiereDate()))
                            + "-"+ android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(item.getEndDate())));
                    layout.addView(date);
                    addSpacer(activity, layout, "    ");
                }
                break;
            case "Series":
                if (item.getProductionYear() != null && item.getProductionYear() > 0) {
                    date.setText(item.getProductionYear().toString());
                    layout.addView(date);
                    addSpacer(activity, layout, "  ");
                }
                break;
            default:
                if (item.getPremiereDate() != null) {
                    date.setText(new SimpleDateFormat("d MMM y").format(Utils.convertToLocalDate(item.getPremiereDate())));
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
            if (width > 1910) {
                addBlockText(activity, layout, "1080");
            } else if (width > 1270) {
                addBlockText(activity, layout, "720");
            } else addBlockText(activity, layout, activity.getString(R.string.lbl_sd));

            addSpacer(activity, layout, "  ");
        }
    }

    private static void addSeriesStatus(Activity activity, BaseItemDto item, LinearLayout layout) {
        if ("Series".equals(item.getType()) && item.getSeriesStatus() != null) {
            boolean continuing = item.getSeriesStatus() == SeriesStatus.Continuing;
            String status = continuing ? activity.getString(R.string.lbl__continuing) : activity.getString(R.string.lbl_ended);
            addBlockText(activity, layout, status, textSize-4, Color.LTGRAY, continuing ? R.drawable.green_gradient : R.drawable.red_gradient);
            addSpacer(activity, layout, "  ");
        }
    }

    private static void addMediaDetails(Activity activity, BaseItemDto item, LinearLayout layout) {
        MediaStream stream = Utils.GetFirstAudioStream(item);

        if (stream != null) {
            if (stream.getCodec() != null) {
                String codec = stream.getCodec().equals("dca") ? "DTS" : stream.getCodec().equals("ac3") ? "Dolby" : stream.getCodec().toUpperCase();
                addBlockText(activity, layout, codec);
                addSpacer(activity, layout, " ");
            }
            if (stream.getChannelLayout() != null) {
                addBlockText(activity, layout, stream.getChannelLayout().toUpperCase());
                addSpacer(activity, layout, "  ");
            }
        }
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text) {
        addBlockText(activity, layout, text, textSize-4);
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text, int size) {
        addBlockText(activity, layout, text, size, Color.DKGRAY, R.drawable.gray_gradient);
    }

    public static void addBlockText(Activity activity, LinearLayout layout, String text, int size, int textColor, int backgroundRes) {
        TextView view = new TextView(activity);
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setText(" " + text + " ");
        view.setBackgroundResource(backgroundRes);
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

    public static void addResourceImage(Activity activity, LinearLayout layout, int imgResource, int width, int height) {
        ImageView image = new ImageView(activity);
        image.setImageResource(imgResource);
        if (width > 0) image.setMaxWidth(width);
        if (height > 0) image.setMaxHeight(height);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        layout.addView(image);
    }


}
