package tv.mediabrowser.mediabrowsertv;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateUtils;

import com.jakewharton.disklrucache.Util;

import java.text.SimpleDateFormat;

import mediabrowser.model.apiclient.ServerInfo;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ChapterInfoDto;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.ChapterInfo;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.entities.LocationType;
import mediabrowser.model.search.SearchHint;

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
    private ItemType type;
    private boolean preferParentThumb = false;


    public BaseRowItem(int index, BaseItemDto item) {
        this(index, item, false);
    }

    public BaseRowItem(int index, BaseItemDto item, boolean preferParentThumb) {
        this.index = index;
        this.baseItem = item;
        type = ItemType.BaseItem;
        this.preferParentThumb = preferParentThumb;
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

    public boolean getIsChapter() { return type == ItemType.Chapter; }
    public boolean getIsPerson() { return type == ItemType.Person; }
    public boolean getIsBaseItem() { return type == ItemType.BaseItem; }
    public ItemType getItemType() { return type; }

    public String getPrimaryImageUrl(int maxHeight) {
        switch (type) {

            case BaseItem:
                return Utils.getPrimaryImageUrl(baseItem, TvApp.getApplication().getApiClient(),true, preferParentThumb, maxHeight);
            case Person:
                return Utils.getPrimaryImageUrl(person, TvApp.getApplication().getApiClient(), maxHeight);
            case User:
                return Utils.getPrimaryImageUrl(user, TvApp.getApplication().getLoginApiClient());
            case Chapter:
                return chapterInfo.getImagePath();
            case Server:
                return "android.resource://tv.mediabrowser.mediabrowsertv/" + R.drawable.server;
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
            case SearchHint:
                return (searchHint.getSeries() != null ? searchHint.getSeries() + " - " : "") + searchHint.getName();
        }

        return "<Unknown>";
    }

    public String getSubText() {
        switch (type) {

            case BaseItem:
                return (baseItem.getType().equals("Episode") && baseItem.getLocationType().equals(LocationType.Virtual)) ? new SimpleDateFormat("d MMM y").format(Utils.convertToLocalDate(baseItem.getPremiereDate())) : baseItem.getOfficialRating();
            case Person:
                return person.getRole();
            case Chapter:
                Long pos = chapterInfo.getStartPositionTicks() / 10000;
                return Utils.formatMillis(pos.intValue());
            case Server:
                return serverInfo.getLocalAddress().substring(7);
            case User:
                return DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(user.getLastActivityDate()).getTime()).toString();
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
                if (!baseItem.getIsFolder() && baseItem.getCriticRating() != null) {
                    return baseItem.getCriticRating() > 59 ? TvApp.getApplication().getDrawable(R.drawable.fresh) : TvApp.getApplication().getDrawable(R.drawable.rotten);
                }
                break;
            case Person:
                break;
            case Server:
                break;
            case User:
                if (user.getHasPassword()) {
                    return TvApp.getApplication().getDrawable(R.drawable.lock);
                }
                break;
            case Chapter:
                break;
        }

        return null;
    }

    public enum ItemType {
        BaseItem,
        Person,
        Server, User, Chapter, SearchHint
    }
}

