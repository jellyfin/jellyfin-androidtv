package org.jellyfin.androidtv.data.compat;

import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod;

@Deprecated
public class SubtitleStreamInfo {
    private String Url;

    public final String getUrl() {
        return Url;
    }

    public final void setUrl(String value) {
        Url = value;
    }

    private String Language;

    public final String getLanguage() {
        return Language;
    }

    public final void setLanguage(String value) {
        Language = value;
    }

    private String Name;

    public final String getName() {
        return Name;
    }

    public final void setName(String value) {
        Name = value;
    }

    private boolean IsForced;

    public final boolean getIsForced() {
        return IsForced;
    }

    public final void setIsForced(boolean value) {
        IsForced = value;
    }

    private String Format;

    public final String getFormat() {
        return Format;
    }

    public final void setFormat(String value) {
        Format = value;
    }

    private String DisplayTitle;

    public final String getDisplayTitle() {
        return DisplayTitle;
    }

    public final void setDisplayTitle(String value) {
        DisplayTitle = value;
    }

    private int Index;

    public final int getIndex() {
        return Index;
    }

    public final void setIndex(int value) {
        Index = value;
    }

    private SubtitleDeliveryMethod DeliveryMethod = SubtitleDeliveryMethod.values()[0];

    public final SubtitleDeliveryMethod getDeliveryMethod() {
        return DeliveryMethod;
    }

    public final void setDeliveryMethod(SubtitleDeliveryMethod value) {
        DeliveryMethod = value;
    }

    private boolean IsExternalUrl;

    public final boolean getIsExternalUrl() {
        return IsExternalUrl;
    }

    public final void setIsExternalUrl(boolean value) {
        IsExternalUrl = value;
    }
}
