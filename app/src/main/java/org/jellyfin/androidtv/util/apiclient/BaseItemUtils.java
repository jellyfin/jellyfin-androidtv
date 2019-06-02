package org.jellyfin.androidtv.util.apiclient;

import android.app.Application;
import android.text.format.DateFormat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.model.ChapterItemInfo;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.ChapterInfoDto;
import org.jellyfin.apiclient.model.dto.ImageOptions;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.entities.LocationType;
import org.jellyfin.apiclient.model.library.PlayAccess;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;

public class BaseItemUtils {
    public static boolean isLiveTv(BaseItemDto item) {
        return "Program".equals(item.getType()) || "LiveTvChannel".equals(item.getType());
    }

    public static boolean canPlay(BaseItemDto item) {
        return item.getPlayAccess().equals(PlayAccess.Full)
                && ((item.getIsPlaceHolder() == null || !item.getIsPlaceHolder())
                && (!item.getType().equals("Episode") || !item.getLocationType().equals(LocationType.Virtual)))
                && (!item.getType().equals("Person"))
                && (!item.getType().equals("SeriesTimer"))
                && (!item.getIsFolderItem() || item.getChildCount() == null || item.getChildCount() > 0);
    }

    public static String getFullName(BaseItemDto item) {
        switch (item.getType()) {
            case "Episode":
                return item.getSeriesName() + (item.getParentIndexNumber() != null ? " S" + item.getParentIndexNumber() : "") + (item.getIndexNumber() != null ? " E" + item.getIndexNumber() : "") + (item.getIndexNumberEnd() != null ? "-" + item.getIndexNumberEnd() : "");
            case "Audio":
            case "MusicAlbum":
                // we actually want the artist name if available
                return (item.getAlbumArtist() != null ? item.getAlbumArtist() + " - " : "") + item.getName();
            default:
                return item.getName();
        }
    }

    public static String getSubName(BaseItemDto item) {
        switch (item.getType()) {
            case "Episode":
                String addendum = item.getLocationType().equals(LocationType.Virtual) && item.getPremiereDate() != null ? " (" +  TimeUtils.getFriendlyDate(TimeUtils.convertToLocalDate(item.getPremiereDate())) + ")" : "";
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

    public static String getProgramSubText(BaseItemDto baseItem) {
        StringBuilder builder = new StringBuilder();
        // Add the channel name if set
        if (baseItem.getChannelName() != null) {
            builder.append(baseItem.getChannelName())
                    .append(" - ");
        }
        // Add the episode title if set
        if (baseItem.getEpisodeTitle() != null) {
            builder.append(baseItem.getEpisodeTitle())
                    .append(" ");
        }

        Calendar startTime = Calendar.getInstance();
        startTime.setTime(TimeUtils.convertToLocalDate(baseItem.getStartDate()));
        // If the start time is on a different day, add the date
        if (startTime.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
            builder.append(TimeUtils.getFriendlyDate(startTime.getTime()))
                    .append(" ");
        }
        // Add the start and end time
        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(TvApp.getApplication());
        builder.append(dateFormat.format(startTime.getTime()))
                .append("-")
                .append(dateFormat.format(TimeUtils.convertToLocalDate(baseItem.getEndDate())));

        return builder.toString();
    }

    public static BaseItemPerson getFirstPerson(BaseItemDto item, String type) {
        if (item.getPeople() != null && item.getPeople().length > 0) {
            for (BaseItemPerson person : item.getPeople()) {
                if (type.equals(person.getType())) {
                    return person;
                }
            }
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

    public static boolean isNew(BaseItemDto program) {
        return Utils.isTrue(program.getIsSeries()) && !Utils.isTrue(program.getIsNews()) && !Utils.isTrue(program.getIsRepeat());
    }

    public static String getSeriesOverview(SeriesTimerInfoDto timer) {
        Application application = TvApp.getApplication();
        StringBuilder builder = new StringBuilder();
        builder.append(application.getString(R.string.msg_will_record))
                .append(" ");
        if (Utils.isTrue(timer.getRecordNewOnly())) {
            builder.append(application.getString(R.string.lbl_only_new_episodes));
        } else {
            builder.append(application.getString(R.string.lbl_all_episodes));
        }
        builder.append("\n")
                .append(application.getString(R.string.lbl_on))
                .append(" ");
        if (Utils.isTrue(timer.getRecordAnyChannel())) {
            builder.append(application.getString(R.string.lbl_any_channel));
        } else {
            builder.append(timer.getChannelName());
        }
        builder.append("\n")
                .append(timer.getDayPattern());
        if (Utils.isTrue(timer.getRecordAnyTime())) {
            builder.append(" ")
                    .append(application.getString(R.string.lbl_at_any_time));
        }
        builder.append("\n")
                .append("Starting");
        if (timer.getPrePaddingSeconds() > 0) {
            builder.append(TimeUtils.formatSeconds(timer.getPrePaddingSeconds()))
                    .append(" Early");
        } else {
            builder.append("On Schedule");
        }
        builder.append(" And Ending ");
        if (timer.getPostPaddingSeconds() > 0) {
            builder.append(TimeUtils.formatSeconds(timer.getPostPaddingSeconds()))
                    .append(" After Schedule");
        } else {
            builder.append("On Schedule");
        }
        return builder.toString();
    }
}
