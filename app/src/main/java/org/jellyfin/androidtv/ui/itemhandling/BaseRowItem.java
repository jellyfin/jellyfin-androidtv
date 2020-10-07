package org.jellyfin.androidtv.ui.itemhandling;

import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;

import androidx.core.content.ContextCompat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.data.model.ChapterItemInfo;
import org.jellyfin.androidtv.ui.GridButton;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.TimeUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.BaseItemUtils;
import org.jellyfin.apiclient.interaction.ApiClient;
import org.jellyfin.apiclient.interaction.EmptyResponse;
import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.apiclient.ServerInfo;
import org.jellyfin.apiclient.model.dto.BaseItemDto;
import org.jellyfin.apiclient.model.dto.BaseItemPerson;
import org.jellyfin.apiclient.model.dto.BaseItemType;
import org.jellyfin.apiclient.model.dto.UserDto;
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.search.SearchHint;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import kotlin.Lazy;

import static org.koin.java.KoinJavaComponent.inject;

public class BaseRowItem {
    private int index;
    private BaseItemDto baseItem;
    private BaseItemPerson person;
    private ChapterItemInfo chapterInfo;
    private ServerInfo serverInfo;
    private UserDto user;
    private SearchHint searchHint;
    private ChannelInfoDto channelInfo;
    private SeriesTimerInfoDto seriesTimerInfo;
    private GridButton gridButton;
    private ItemType type;
    private boolean preferParentThumb = false;
    protected boolean staticHeight = false;
    private SelectAction selectAction = SelectAction.ShowDetails;
    private boolean isPlaying;

    private Lazy<ApiClient> apiClient = inject(ApiClient.class);

    public BaseRowItem(int index, BaseItemDto item) {
        this(index, item, false, false);
    }

    public BaseRowItem(int index, BaseItemDto item, boolean preferParentThumb, boolean staticHeight) {
        this(index, item, preferParentThumb, staticHeight, SelectAction.ShowDetails);
    }

    public BaseRowItem(int index, BaseItemDto item, boolean preferParentThumb, boolean staticHeight, SelectAction selectAction) {
        this.index = index;
        this.baseItem = item;
        this.type = item.getBaseItemType() == BaseItemType.Program ? ItemType.LiveTvProgram : item.getBaseItemType() == BaseItemType.Recording ? ItemType.LiveTvRecording : ItemType.BaseItem;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.selectAction = selectAction;
    }

    public BaseRowItem(int index, ChannelInfoDto channel) {
        this.index = index;
        this.channelInfo = channel;
        this.type = ItemType.LiveTvChannel;
    }

    public BaseRowItem(BaseItemDto program, boolean staticHeight) {
        this(0, program, false, staticHeight);
    }

    public BaseRowItem(BaseItemDto program) {
        this(0, program);
    }

    public BaseRowItem(ServerInfo server) {
        this.serverInfo = server;
        this.type = ItemType.Server;
    }

    public BaseRowItem(SeriesTimerInfoDto timer) {
        this.seriesTimerInfo = timer;
        this.type = ItemType.SeriesTimer;
    }

    public BaseRowItem(BaseItemPerson person) {
        this.person = person;
        this.staticHeight = true;
        this.type = ItemType.Person;
    }

    public BaseRowItem(UserDto user) {
        this.user = user;
        this.type = ItemType.User;
    }

    public BaseRowItem(SearchHint hint) {
        this.searchHint = hint;
        type = ItemType.SearchHint;
    }

    public BaseRowItem(ChapterItemInfo chapter) {
        this.chapterInfo = chapter;
        this.staticHeight = true;
        this.type = ItemType.Chapter;
    }

    public BaseRowItem(GridButton button) {
        this.gridButton = button;
        this.type = ItemType.GridButton;
        this.staticHeight = true;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int ndx) {
        index = ndx;
    }

    public BaseItemDto getBaseItem() {
        return baseItem;
    }

    public BaseItemPerson getPerson() {
        return person;
    }

    public ChapterItemInfo getChapterInfo() {
        return chapterInfo;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public UserDto getUser() {
        return user;
    }

    public SearchHint getSearchHint() {
        return searchHint;
    }

    public ChannelInfoDto getChannelInfo() {
        return channelInfo;
    }

    public BaseItemDto getProgramInfo() {
        return baseItem;
    }

    public BaseItemDto getRecordingInfo() {
        return baseItem;
    }

    public SeriesTimerInfoDto getSeriesTimerInfo() {
        return seriesTimerInfo;
    }

    public GridButton getGridButton() {
        return gridButton;
    }

    public boolean isChapter() {
        return type == ItemType.Chapter;
    }

    public boolean isPerson() {
        return type == ItemType.Person;
    }

    public boolean isBaseItem() {
        return type == ItemType.BaseItem;
    }

    public boolean getPreferParentThumb() {
        return preferParentThumb;
    }

    public ItemType getItemType() {
        return type;
    }

    public boolean isFolder() {
        return type == ItemType.BaseItem && baseItem != null && baseItem.getIsFolderItem();
    }

    public boolean showCardInfoOverlay() {
        return type == ItemType.BaseItem && baseItem != null
                && Arrays.asList(BaseItemType.Folder, BaseItemType.PhotoAlbum, BaseItemType.RecordingGroup,
                BaseItemType.UserView, BaseItemType.CollectionFolder, BaseItemType.Photo,
                BaseItemType.Video, BaseItemType.Person, BaseItemType.Playlist,
                BaseItemType.MusicArtist).contains(baseItem.getBaseItemType());
    }

    public boolean isValid() {
        switch (type) {
            case BaseItem:
                return baseItem != null;
            case Person:
                return person != null;
            case Chapter:
                return chapterInfo != null;
            case SeriesTimer:
                return seriesTimerInfo != null;
            default:
                // compatibility
                return true;
        }
    }

    public String getImageUrl(String imageType, int maxHeight) {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                switch (imageType) {
                    case org.jellyfin.androidtv.constant.ImageType.BANNER:
                        return ImageUtils.getBannerImageUrl(baseItem, apiClient.getValue(), maxHeight);
                    case org.jellyfin.androidtv.constant.ImageType.THUMB:
                        return ImageUtils.getThumbImageUrl(baseItem, apiClient.getValue(), maxHeight);
                    default:
                        return getPrimaryImageUrl(maxHeight);
                }
            default:
                return getPrimaryImageUrl(maxHeight);
        }
    }

    public String getPrimaryImageUrl(int maxHeight) {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                return ImageUtils.getPrimaryImageUrl(baseItem, apiClient.getValue(), preferParentThumb, maxHeight);
            case Person:
                return ImageUtils.getPrimaryImageUrl(person, apiClient.getValue(), maxHeight);
            case User:
                return ImageUtils.getPrimaryImageUrl(user, apiClient.getValue());
            case Chapter:
                return chapterInfo.getImagePath();
            case LiveTvChannel:
                return ImageUtils.getPrimaryImageUrl(channelInfo, apiClient.getValue());
            case Server:
                return ImageUtils.getResourceUrl(R.drawable.tile_port_server);
            case GridButton:
                return ImageUtils.getResourceUrl(gridButton.getImageIndex());
            case SeriesTimer:
                return ImageUtils.getResourceUrl(R.drawable.tile_land_series_timer);
            case SearchHint:
                if (Utils.isNonEmpty(searchHint.getPrimaryImageTag())) {
                    return ImageUtils.getImageUrl(searchHint.getItemId(), ImageType.Primary, searchHint.getPrimaryImageTag(), apiClient.getValue());
                } else if (Utils.isNonEmpty(searchHint.getThumbImageItemId())) {
                    return ImageUtils.getImageUrl(searchHint.getThumbImageItemId(), ImageType.Thumb, searchHint.getThumbImageTag(), apiClient.getValue());
                }
        }
        return null;
    }

    public boolean isFavorite() {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getUserData() != null && baseItem.getUserData().getIsFavorite();
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
        }

        return false;
    }

    public boolean isPlayed() {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getUserData() != null && baseItem.getUserData().getPlayed();
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
        }

        return false;
    }

    public String getCardName() {
        switch (type) {
            case BaseItem:
                if (baseItem.getBaseItemType() == BaseItemType.Audio) {
                    return baseItem.getAlbumArtist() != null ? baseItem.getAlbumArtist() : baseItem.getAlbum() != null ? baseItem.getAlbum() : "<Unknown>";
                }
            default:
                return getFullName();
        }
    }

    public String getFullName() {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                return BaseItemUtils.getFullName(baseItem);
            case Person:
                return person.getName();
            case Chapter:
                return chapterInfo.getName();
            case Server:
                return serverInfo.getName();
            case User:
                return user.getName();
            case LiveTvChannel:
                return channelInfo.getName();
            case GridButton:
                return gridButton.getText();
            case SeriesTimer:
                return seriesTimerInfo.getName();
            case SearchHint:
                return (searchHint.getSeries() != null ? searchHint.getSeries() + " - " : "") + searchHint.getName();
        }

        return TvApp.getApplication().getString(R.string.lbl_bracket_unknown);
    }

    public String getName() {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getBaseItemType() == BaseItemType.Audio ? getFullName() : baseItem.getName();
            case Person:
                return person.getName();
            case Server:
                return serverInfo.getName();
            case User:
                return user.getName();
            case Chapter:
                return chapterInfo.getName();
            case SearchHint:
                return searchHint.getName();
            case LiveTvChannel:
                return channelInfo.getName();
            case GridButton:
                return gridButton.getText();
            case SeriesTimer:
                return seriesTimerInfo.getName();
        }

        return TvApp.getApplication().getString(R.string.lbl_bracket_unknown);
    }

    public String getItemId() {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                return baseItem.getId();
            case Person:
                return person.getId();
            case Chapter:
                return chapterInfo.getItemId();
            case Server:
                return serverInfo.getId();
            case User:
                return user.getId();
            case LiveTvChannel:
                return channelInfo.getId();
            case GridButton:
                return null;
            case SearchHint:
                return searchHint.getItemId();
            case SeriesTimer:
                return seriesTimerInfo.getId();
        }

        return null;
    }

    public String getSubText() {
        switch (type) {
            case BaseItem:
                return BaseItemUtils.getSubName(baseItem);
            case Person:
                return person.getRole();
            case Chapter:
                Long pos = chapterInfo.getStartPositionTicks() / 10000;
                return TimeUtils.formatMillis(pos.intValue());
            case Server:
                return serverInfo.getAddress() != null ? serverInfo.getAddress().substring(7) : "";
            case LiveTvChannel:
                return channelInfo.getNumber();
            case LiveTvProgram:
                return baseItem.getEpisodeTitle() != null ? baseItem.getEpisodeTitle() : baseItem.getChannelName();
            case LiveTvRecording:
                return (baseItem.getChannelName() != null ? baseItem.getChannelName() + " - " : "") + (baseItem.getEpisodeTitle() != null ? baseItem.getEpisodeTitle() : "") + " " +
                        new SimpleDateFormat("d MMM").format(TimeUtils.convertToLocalDate(baseItem.getStartDate())) + " " +
                        (android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(baseItem.getStartDate())) + "-"
                                + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(TimeUtils.convertToLocalDate(baseItem.getEndDate())));
            case User:
                Date date = user.getLastActivityDate();
                return date != null ? DateUtils.getRelativeTimeSpanString(TimeUtils.convertToLocalDate(date).getTime()).toString() : TvApp.getApplication().getString(R.string.lbl_never);
            case SearchHint:
                return searchHint.getType();
            case SeriesTimer:
                return (Utils.isTrue(seriesTimerInfo.getRecordAnyChannel()) ? "All Channels" : seriesTimerInfo.getChannelName()) + " " + seriesTimerInfo.getDayPattern();
        }

        return "";
    }

    public BaseItemType getBaseItemType() {
        if (baseItem != null)
            return baseItem.getBaseItemType();
        else
            return null;
    }

    public String getSummary() {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getOverview();
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
            case SeriesTimer:
                return BaseItemUtils.getSeriesOverview(seriesTimerInfo);
        }

        return "";
    }

    public long getRuntimeTicks() {
        switch (type) {
            case LiveTvRecording:
            case BaseItem:
                return baseItem.getRunTimeTicks() != null ? baseItem.getRunTimeTicks() : 0;
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
            case LiveTvProgram:
                return ((baseItem.getStartDate() != null) && (baseItem.getEndDate() != null)) ? (baseItem.getEndDate().getTime() - (baseItem.getStartDate().getTime() * 10000)) : 0;
        }

        return 0;
    }

    public int getChildCount() {
        switch (type) {
            case BaseItem:
                return isFolder() && baseItem.getBaseItemType() != BaseItemType.MusicArtist && baseItem.getChildCount() != null ? baseItem.getChildCount() : -1;
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case LiveTvRecording:
            case GridButton:
            case LiveTvProgram:
                break;
        }

        return -1;
    }

    public String getChildCountStr() {
        if (baseItem != null && baseItem.getBaseItemType() == BaseItemType.Playlist && baseItem.getCumulativeRunTimeTicks() != null) {
            return TimeUtils.formatMillis(baseItem.getCumulativeRunTimeTicks() / 10000);
        } else {
            Integer count = getChildCount();
            return count > 0 ? count.toString() : "";
        }
    }

    public String getBackdropImageUrl() {
        if (type == ItemType.BaseItem) {
            return ImageUtils.getBackdropImageUrl(baseItem, apiClient.getValue(), true);
        }

        return null;
    }

    public Drawable getBadgeImage() {
        switch (type) {
            case BaseItem:
                if (baseItem.getBaseItemType() == BaseItemType.Movie && baseItem.getCriticRating() != null) {
                    return baseItem.getCriticRating() > 59 ? ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.fresh) : ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.rotten);
                } else if (baseItem.getBaseItemType() == BaseItemType.Program && baseItem.getTimerId() != null) {
                    return baseItem.getSeriesTimerId() != null ? ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.ic_record_series_red) : ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.ic_record_red);
                }
                break;
            case Person:
            case Server:
                break;
            case User:
                if (user.getHasPassword()) {
                    return ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.ic_lock);
                }
                break;
            case LiveTvProgram:
                if (baseItem.getTimerId() != null) {
                    return baseItem.getSeriesTimerId() != null ? ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.ic_record_series_red) : ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.ic_record_red);
                }
            case Chapter:
                break;
        }

        return ContextCompat.getDrawable(TvApp.getApplication(), R.drawable.blank10x10);
    }

    public void refresh(final EmptyResponse outerResponse) {
        switch (type) {
            case BaseItem:
                apiClient.getValue().GetItemAsync(getItemId(), TvApp.getApplication().getCurrentUser().getId(), new Response<BaseItemDto>() {
                    @Override
                    public void onResponse(BaseItemDto response) {
                        baseItem = response;
                        outerResponse.onResponse();
                    }
                });
                break;
            case Person:
            case Server:
            case User:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case LiveTvRecording:
            case GridButton:
            case LiveTvProgram:
                break;
        }
    }

    public SelectAction getSelectAction() {
        return selectAction;
    }

    public boolean isStaticHeight() {
        return staticHeight;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean value) {
        isPlaying = value;
    }

    public enum ItemType {
        BaseItem,
        Person,
        Server,
        User,
        Chapter,
        SearchHint,
        LiveTvChannel,
        LiveTvRecording,
        GridButton,
        SeriesTimer,
        LiveTvProgram,

    }

    public enum SelectAction {
        ShowDetails,
        Play
    }
}
