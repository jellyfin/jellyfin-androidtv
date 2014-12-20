package tv.mediabrowser.mediabrowsertv;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.ChapterInfoDto;

/**
 * Created by Eric on 12/15/2014.
 */
public class BaseRowItem {
    private int index;
    private BaseItemDto baseItem;
    private BaseItemPerson person;
    private ChapterInfoDto chapterInfo;
    private ItemType type;

    public BaseRowItem(int index, BaseItemDto item) {
        this.index = index;
        this.baseItem = item;
        type = ItemType.BaseItem;
    }

    public BaseRowItem(BaseItemPerson person) {
        this.person = person;
        type = ItemType.Person;
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
            case Chapter:
                break;
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
        }

        return "";
    }

    public enum ItemType {
        BaseItem,
        Person,
        Chapter
    }
}

