package org.jellyfin.androidtv.data.compat;

import org.jellyfin.androidtv.util.Utils;

import timber.log.Timber;

/**
 * Struct ImageSize
 *
 * @deprecated
 */
//C# TO JAVA CONVERTER WARNING: Java does not allow user-defined value types. The behavior of this class will differ from the original:
//ORIGINAL LINE: public struct ImageSize
@Deprecated
public final class ImageSize {
    private double _height;
    private double _width;

    /**
     * Gets or sets the height.
     *
     * <value>The height.</value>
     */
    public double getHeight() {
        return _height;
    }

    public void setHeight(double value) {
        _height = value;
    }

    /**
     * Gets or sets the width.
     *
     * <value>The width.</value>
     */
    public double getWidth() {
        return _width;
    }

    public void setWidth(double value) {
        _width = value;
    }

    public boolean equals(ImageSize size) {
        return (new Double(getWidth())).equals(size.getWidth()) && (new Double(getHeight())).equals(size.getHeight());
    }

    @Override
    public String toString() {
        return String.format("%1$s-%2$s", getWidth(), getHeight());
    }

    public ImageSize() {
    }

    public ImageSize(String value) {
        _width = 0;

        _height = 0;

        ParseValue(value);
    }

    public ImageSize(int width, int height) {
        _width = width;
        _height = height;
    }

    private void ParseValue(String value) {
        if (Utils.isNonEmpty(value)) {
            String[] parts = value.split("[-]", -1);

            if (parts.length == 2) {
                try {
                    _width = Double.parseDouble(parts[0]);
                    _height = Double.parseDouble(parts[1]);
                } catch (NumberFormatException ex) {
                    Timber.e(ex, "Could not parse width and height");
                }
            }
        }
    }

    public ImageSize clone() {
        ImageSize varCopy = new ImageSize();

        varCopy._height = this._height;
        varCopy._width = this._width;

        return varCopy;
    }
}
