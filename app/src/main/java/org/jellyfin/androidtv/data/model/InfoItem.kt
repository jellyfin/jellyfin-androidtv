package org.jellyfin.androidtv.data.model;

public class InfoItem {
    private String label;
    private String value;

    public InfoItem() {
        this("", "");
    }

    public InfoItem(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
