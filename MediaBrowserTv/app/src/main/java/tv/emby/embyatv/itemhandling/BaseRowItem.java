package tv.emby.embyatv.itemhandling;

import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.livetv.ChannelInfoDto;
import mediabrowser.model.livetv.ProgramInfoDto;
import mediabrowser.model.livetv.RecordingInfoDto;
import mediabrowser.model.search.SearchHint;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.model.ChapterItemInfo;
import tv.emby.embyatv.ui.GridButton;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 12/15/2014.
 */
public class BaseRowItem {
    private int index;
    private BaseItemDto baseItem;
    private BaseItemPerson person;
    private ChapterItemInfo chapterInfo;
    private ServerInfo serverInfo;
    private UserDto user;
    private SearchHint searchHint;
    private ChannelInfoDto channelInfo;
    private ProgramInfoDto programInfo;
    private RecordingInfoDto recordingInfo;
    private GridButton gridButton;
    private ItemType type;
    private boolean preferParentThumb = false;
    private boolean staticHeight = false;
    private SelectAction selectAction = SelectAction.ShowDetails;


    public BaseRowItem(int index, BaseItemDto item) {
        this(index, item, false, false);
    }

    public BaseRowItem(int index, BaseItemDto item, boolean preferParentThumb, boolean staticHeight) {
        this(index, item, preferParentThumb, staticHeight, SelectAction.ShowDetails);
    }
    public BaseRowItem(int index, BaseItemDto item, boolean preferParentThumb, boolean staticHeight, SelectAction selectAction) {
        this.index = index;
        this.baseItem = item;
        type = ItemType.BaseItem;
        this.preferParentThumb = preferParentThumb;
        this.staticHeight = staticHeight;
        this.selectAction = selectAction;
    }

    public BaseRowItem(int index, ChannelInfoDto channel) {
        this.index = index;
        this.channelInfo = channel;
        type = ItemType.LiveTvChannel;
    }

    public BaseRowItem(ProgramInfoDto program) {
        this.programInfo = program;
        type = ItemType.LiveTvProgram;
    }

    public BaseRowItem(RecordingInfoDto program) {
        this.recordingInfo = program;
        type = ItemType.LiveTvRecording;
    }

    public BaseRowItem(ServerInfo server) {
        this.serverInfo = server;
        this.type = ItemType.Server;
    }

    public BaseRowItem(BaseItemPerson person) {
        this.person = person;
        type = ItemType.Person;
    }

    public BaseRowItem(UserDto user) {
        this.user = user;
        type = ItemType.User;
    }

    public BaseRowItem(SearchHint hint) {
        this.searchHint = hint;
        type = ItemType.SearchHint;
    }

    public BaseRowItem(ChapterItemInfo chapter) {
        this.chapterInfo = chapter;
        type = ItemType.Chapter;
    }

    public BaseRowItem(GridButton button) {
        this.gridButton = button;
        type = ItemType.GridButton;
    }

    public int getIndex() {
        return index;
    }

    public BaseItemDto getBaseItem() {
        return baseItem;
    }
    public BaseItemPerson getPerson() { return person; }
    public ChapterItemInfo getChapterInfo() { return chapterInfo; }
    public ServerInfo getServerInfo() { return serverInfo; }
    public UserDto getUser() { return user; }
    public SearchHint getSearchHint() { return searchHint; }
    public ChannelInfoDto getChannelInfo() { return channelInfo; }
    public ProgramInfoDto getProgramInfo() { return programInfo; }
    public RecordingInfoDto getRecordingInfo() { return recordingInfo; }

    public boolean getIsChapter() { return type == ItemType.Chapter; }
    public boolean getIsPerson() { return type == ItemType.Person; }
    public boolean getIsBaseItem() { return type == ItemType.BaseItem; }
    public boolean getPreferParentThumb() { return preferParentThumb; }
    public ItemType getItemType() { return type; }

    public String getPrimaryImageUrl(int maxHeight) {
        switch (type) {

            case BaseItem:
                return Utils.getPrimaryImageUrl(baseItem, TvApp.getApplication().getApiClient(), true, preferParentThumb, maxHeight);
            case Person:
                return Utils.getPrimaryImageUrl(person, TvApp.getApplication().getApiClient(), maxHeight);
            case User:
                return Utils.getPrimaryImageUrl(user, TvApp.getApplication().getLoginApiClient());
            case Chapter:
                return chapterInfo.getImagePath();
            case LiveTvChannel:
                return Utils.getPrimaryImageUrl(channelInfo, TvApp.getApplication().getApiClient());
            case LiveTvProgram:
                return Utils.getPrimaryImageUrl(programInfo, TvApp.getApplication().getApiClient());
            case LiveTvRecording:
                return Utils.getPrimaryImageUrl(recordingInfo, TvApp.getApplication().getApiClient());
            case Server:
                return "android.resource://tv.emby.embyatv/" + R.drawable.server;
            case GridButton:
                return "android.resource://tv.emby.embyatv/" + gridButton.getImageIndex();
            case SearchHint:
                return !Utils.IsEmpty(searchHint.getPrimaryImageTag()) ? Utils.getImageUrl(searchHint.getItemId(), ImageType.Primary, searchHint.getPrimaryImageTag(), TvApp.getApplication().getApiClient()) :
                        !Utils.IsEmpty(searchHint.getThumbImageItemId()) ? Utils.getImageUrl(searchHint.getThumbImageItemId(), ImageType.Thumb, searchHint.getThumbImageTag(), TvApp.getApplication().getApiClient()) : null;
        }
        return null;
    }

    public String getFullName() {
        switch (type) {

            case BaseItem:
                return Utils.GetFullName(baseItem);
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
            case LiveTvProgram:
                return programInfo.getName();
            case LiveTvRecording:
                return recordingInfo.getName();
            case GridButton:
                return gridButton.getText();
            case SearchHint:
                return (searchHint.getSeries() != null ? searchHint.getSeries() + " - " : "") + searchHint.getName();
        }

        return TvApp.getApplication().getString(R.string.lbl_bracket_unknown);
    }

    public String getSubText() {
        switch (type) {

            case BaseItem:
                return Utils.GetSubName(baseItem);
            case Person:
                return person.getRole();
            case Chapter:
                Long pos = chapterInfo.getStartPositionTicks() / 10000;
                return Utils.formatMillis(pos.intValue());
            case Server:
                return serverInfo.getLocalAddress().substring(7);
            case LiveTvChannel:
                return channelInfo.getNumber();
            case LiveTvProgram:
                return (programInfo.getEpisodeTitle() != null ? programInfo.getEpisodeTitle() : programInfo.getChannelName()) + " " +
                        (android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(programInfo.getStartDate())) + "-"
                        + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(programInfo.getEndDate())));
            case LiveTvRecording:
                return (recordingInfo.getEpisodeTitle() != null ? recordingInfo.getEpisodeTitle() : recordingInfo.getChannelName()) + " " +
                        new SimpleDateFormat("d MMM").format(Utils.convertToLocalDate(recordingInfo.getStartDate())) + " " +
                        (android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(recordingInfo.getStartDate())) + "-"
                                + android.text.format.DateFormat.getTimeFormat(TvApp.getApplication()).format(Utils.convertToLocalDate(recordingInfo.getEndDate())));
            case User:
                Date date = user.getLastActivityDate();
                return date != null ? DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(date).getTime()).toString() : TvApp.getApplication().getString(R.string.lbl_never);
            case SearchHint:
                return searchHint.getType();
        }

        return "";
    }

    public String getBackdropImageUrl() {
        switch (type) {
            case BaseItem:
                return Utils.getBackdropImageUrl(baseItem, TvApp.getApplication().getConnectionManager().GetApiClient(baseItem), true);

        }

        return null;
    }

    public Drawable getBadgeImage() {
        switch (type) {

            case BaseItem:
                if (baseItem.getType().equals("Movie") && baseItem.getCriticRating() != null) {
                    return baseItem.getCriticRating() > 59 ? TvApp.getApplication().getDrawableCompat(R.drawable.fresh) : TvApp.getApplication().getDrawableCompat(R.drawable.rotten);
                }
                break;
            case Person:
                break;
            case Server:
                break;
            case User:
                if (user.getHasPassword()) {
                    return TvApp.getApplication().getDrawableCompat(R.drawable.lock);
                }
                break;
            case Chapter:
                break;
        }

        return TvApp.getApplication().getDrawableCompat(R.drawable.blank10x10);
    }

    public SelectAction getSelectAction() {
        return selectAction;
    }

    public boolean isStaticHeight() {
        return staticHeight;
    }

    public enum ItemType {
        BaseItem,
        Person,
        Server, User, Chapter, SearchHint, LiveTvChannel, LiveTvRecording, GridButton, LiveTvProgram
    }

    public enum SelectAction {
        ShowDetails,
        Play
    }
}

