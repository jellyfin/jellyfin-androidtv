package org.jellyfin.androidtv.ui.itemhandling;

import static org.koin.java.KoinJavaComponent.inject;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.auth.repository.UserRepository;
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
import org.jellyfin.apiclient.model.entities.ImageType;
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto;
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto;
import org.jellyfin.apiclient.model.search.SearchHint;
import org.jellyfin.sdk.model.api.UserDto;
import org.koin.java.KoinJavaComponent;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import kotlin.Lazy;
import timber.log.Timber;

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

    public BaseRowItem(SeriesTimerInfoDto timer) {
        this.seriesTimerInfo = timer;
        this.type = ItemType.SeriesTimer;
    }

    public BaseRowItem(BaseItemPerson person) {
        this.person = person;
        this.staticHeight = true;
        this.type = ItemType.Person;
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
                && Arrays.asList(BaseItemType.Folder, BaseItemType.PhotoAlbum,
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

    public String getImageUrl(Context context, org.jellyfin.androidtv.constant.ImageType imageType, int maxHeight) {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                switch (imageType) {
                    case BANNER:
                        return ImageUtils.getBannerImageUrl(context, baseItem, apiClient.getValue(), maxHeight);
                    case THUMB:
                        return ImageUtils.getThumbImageUrl(context, baseItem, apiClient.getValue(), maxHeight);
                    default:
                        return getPrimaryImageUrl(context, maxHeight);
                }
            default:
                return getPrimaryImageUrl(context, maxHeight);
        }
    }

    public String getPrimaryImageUrl(Context context, int maxHeight) {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                return ImageUtils.getPrimaryImageUrl(context, baseItem, preferParentThumb, maxHeight);
            case Person:
                return ImageUtils.getPrimaryImageUrl(person, maxHeight);
            case Chapter:
                return chapterInfo.getImagePath();
            case LiveTvChannel:
                return ImageUtils.getPrimaryImageUrl(channelInfo, apiClient.getValue());
            case GridButton:
                return ImageUtils.getResourceUrl(context, gridButton.getImageRes());
            case SeriesTimer:
                return ImageUtils.getResourceUrl(context, R.drawable.tile_land_series_timer);
            case SearchHint:
                if (Utils.isNonEmpty(searchHint.getPrimaryImageTag())) {
                    return ImageUtils.getImageUrl(searchHint.getItemId(), ImageType.Primary, searchHint.getPrimaryImageTag());
                } else if (Utils.isNonEmpty(searchHint.getThumbImageItemId())) {
                    return ImageUtils.getImageUrl(searchHint.getThumbImageItemId(), ImageType.Thumb, searchHint.getThumbImageTag());
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
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
        }

        return false;
    }

    public String getCardName(Context context) {
        switch (type) {
            case BaseItem:
                if (baseItem.getBaseItemType() == BaseItemType.Audio) {
                    return baseItem.getAlbumArtist() != null ? baseItem.getAlbumArtist() : baseItem.getAlbum() != null ? baseItem.getAlbum() : "<Unknown>";
                }
            default:
                return getFullName(context);
        }
    }

    public String getFullName(Context context) {
        switch (type) {
            case BaseItem:
            case LiveTvProgram:
            case LiveTvRecording:
                return BaseItemUtils.getFullName(baseItem, context);
            case Person:
                return person.getName();
            case Chapter:
                return chapterInfo.getName();
            case LiveTvChannel:
                return channelInfo.getName();
            case GridButton:
                return gridButton.getText();
            case SeriesTimer:
                return seriesTimerInfo.getName();
            case SearchHint:
                return (searchHint.getSeries() != null ? searchHint.getSeries() + " - " : "") + searchHint.getName();
        }

        return context.getString(R.string.lbl_bracket_unknown);
    }

    public String getName(Context context) {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getBaseItemType() == BaseItemType.Audio ? getFullName(context) : baseItem.getName();
            case Person:
                return person.getName();
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

        return context.getString(R.string.lbl_bracket_unknown);
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

    public String getSubText(Context context) {
        switch (type) {
            case BaseItem:
                return BaseItemUtils.getSubName(baseItem, context);
            case Person:
                return person.getRole();
            case Chapter:
                Long pos = chapterInfo.getStartPositionTicks() / 10000;
                return TimeUtils.formatMillis(pos.intValue());
            case LiveTvChannel:
                return channelInfo.getNumber();
            case LiveTvProgram:
                return baseItem.getEpisodeTitle() != null ? baseItem.getEpisodeTitle() : baseItem.getChannelName();
            case LiveTvRecording:
                return (baseItem.getChannelName() != null ? baseItem.getChannelName() + " - " : "") + (baseItem.getEpisodeTitle() != null ? baseItem.getEpisodeTitle() : "") + " " +
                        new SimpleDateFormat("d MMM").format(TimeUtils.convertToLocalDate(baseItem.getStartDate())) + " " +
                        (android.text.format.DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(baseItem.getStartDate())) + "-"
                                + android.text.format.DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(baseItem.getEndDate())));
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

    public String getSummary(Context context) {
        switch (type) {
            case BaseItem:
            case LiveTvRecording:
            case LiveTvProgram:
                return baseItem.getOverview();
            case Person:
            case Chapter:
            case SearchHint:
            case LiveTvChannel:
            case GridButton:
                break;
            case SeriesTimer:
                return BaseItemUtils.getSeriesOverview(seriesTimerInfo, context);
        }

        return "";
    }

    public long getRuntimeTicks() {
        switch (type) {
            case LiveTvRecording:
            case BaseItem:
                return baseItem.getRunTimeTicks() != null ? baseItem.getRunTimeTicks() : 0;
            case Person:
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

    public Drawable getBadgeImage(Context context) {
        if (baseItem != null) {
            switch (type) {
                case BaseItem:
                    if (baseItem.getBaseItemType() == BaseItemType.Movie && baseItem.getCriticRating() != null) {
                        return baseItem.getCriticRating() > 59 ? ContextCompat.getDrawable(context, R.drawable.ic_rt_fresh) : ContextCompat.getDrawable(context, R.drawable.ic_rt_rotten);
                    } else if (baseItem.getBaseItemType() == BaseItemType.Program && baseItem.getTimerId() != null) {
                        return baseItem.getSeriesTimerId() != null ? ContextCompat.getDrawable(context, R.drawable.ic_record_series_red) : ContextCompat.getDrawable(context, R.drawable.ic_record_red);
                    }
                    break;
                case Person:
                case LiveTvProgram:
                    if (baseItem.getTimerId() != null) {
                        return baseItem.getSeriesTimerId() != null ? ContextCompat.getDrawable(context, R.drawable.ic_record_series_red) : ContextCompat.getDrawable(context, R.drawable.ic_record_red);
                    }
                case Chapter:
                    break;
            }
        }

        return ContextCompat.getDrawable(context, R.drawable.blank10x10);
    }

    public void refresh(final EmptyResponse outerResponse) {
        switch (type) {
            case BaseItem:
                String id = getItemId();
                UserDto user = KoinJavaComponent.<UserRepository>get(UserRepository.class).getCurrentUser().getValue();
                if (Utils.isNonEmpty(id) && user != null) {
                    apiClient.getValue().GetItemAsync(id, user.getId().toString(), new Response<BaseItemDto>() {
                        @Override
                        public void onResponse(BaseItemDto response) {
                            baseItem = response;
                            outerResponse.onResponse();
                        }
                    });
                } else {
                    Timber.w("BaseRowItem.refresh() failed!");
                }
                break;
            case Person:
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
