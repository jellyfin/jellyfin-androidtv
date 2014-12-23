package tv.mediabrowser.mediabrowsertv;

import android.net.Uri;
import android.text.format.DateUtils;

import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;

/**
 * Created by Eric on 12/15/2014.
 */
public class BaseRowItem {
    private int index;
    private BaseItemDto baseItem;
    private BaseItemPerson person;
    private ChapterInfoDto chapterInfo;
    private ServerInfo serverInfo;
    private UserDto user;
    private ItemType type;

    public BaseRowItem(int index, BaseItemDto item) {
        this.index = index;
        this.baseItem = item;
        type = ItemType.BaseItem;
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

    public BaseRowItem(ChapterInfoDto chapter) {
        this.chapterInfo = chapter;
        type = ItemType.Chapter;
    }

    public int getIndex() {
        return index;
    }

    public BaseItemDto getBaseItem() {
        return baseItem;
    }
    public BaseItemPerson getPerson() { return person; }
    public ChapterInfoDto getChapterInfo() { return chapterInfo; }
    public ServerInfo getServerInfo() { return serverInfo; }
    public UserDto getUser() { return user; }

    public boolean getIsChapter() { return type == ItemType.Chapter; }
    public boolean getIsPerson() { return type == ItemType.Person; }
    public boolean getIsBaseItem() { return type == ItemType.BaseItem; }
    public ItemType getItemType() { return type; }

    public String getPrimaryImageUrl() {
        switch (type) {

            case BaseItem:
                return Utils.getPrimaryImageUrl(baseItem, TvApp.getApplication().getApiClient(),true);
            case Person:
                return Utils.getPrimaryImageUrl(person, TvApp.getApplication().getApiClient());
            case User:
                return Utils.getPrimaryImageUrl(user, TvApp.getApplication().getLoginApiClient());
            case Chapter:
                break;
            case Server:
                return "android.resource://tv.mediabrowser.mediabrowsertv/" + R.drawable.server;
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
        }

        return "<Unknown>";
    }

    public String getSubText() {
        switch (type) {

            case BaseItem:
                return baseItem.getOfficialRating();
            case Person:
                return person.getRole();
            case Chapter:
                Long pos = chapterInfo.getStartPositionTicks() / 10000;
                return Utils.formatMillis(pos.intValue());
            case Server:
                return serverInfo.getLocalAddress().substring(7);
            case User:
                return DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(user.getLastActivityDate()).getTime()).toString();
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

    public enum ItemType {
        BaseItem,
        Person,
        Server, User, Chapter
    }
}

