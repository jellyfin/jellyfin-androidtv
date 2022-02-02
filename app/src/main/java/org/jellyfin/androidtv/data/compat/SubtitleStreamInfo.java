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

    private String Name;

    public final String getName() {
        return Name;
    }

    public final void setName(String value) {
        Name = value;
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
}
