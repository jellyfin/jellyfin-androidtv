package org.jellyfin.androidtv.ui;

public class GridButton {
    private int id;
    private String text;
    private int imageIndex;

    public GridButton(int id, String text, int image) {
        this.id = id;
        this.text = text;
        this.imageIndex = image;
    }

    public String getText() {
        return text;
    }

    public int getId() {
        return id;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    @Override
    public String toString() {
        return text;
    }
}
