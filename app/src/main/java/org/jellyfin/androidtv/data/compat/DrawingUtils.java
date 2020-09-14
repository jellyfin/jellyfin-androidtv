package org.jellyfin.androidtv.data.compat;

/**
 * Class DrawingUtils
 *
 * @deprecated
 */
@Deprecated
public final class DrawingUtils {
    /**
     * Resizes a set of dimensions
     *
     * @param currentWidth  Width of the current.
     * @param currentHeight Height of the current.
     * @param scaleFactor   The scale factor.
     * @return ImageSize.
     */
    public static ImageSize Scale(double currentWidth, double currentHeight, double scaleFactor) {
        ImageSize tempVar = new ImageSize();
        tempVar.setWidth(currentWidth);
        tempVar.setHeight(currentHeight);
        return Scale(tempVar.clone(), scaleFactor);
    }

    /**
     * Resizes a set of dimensions
     *
     * @param size        The size.
     * @param scaleFactor The scale factor.
     * @return ImageSize.
     */
    public static ImageSize Scale(ImageSize size, double scaleFactor) {
        double newWidth = size.getWidth() * scaleFactor;

        return Resize(size.getWidth(), size.getHeight(), newWidth, null, null, null);
    }

    /**
     * Resizes a set of dimensions
     *
     * @param currentWidth  Width of the current.
     * @param currentHeight Height of the current.
     * @param width         The width.
     * @param height        The height.
     * @param maxWidth      A max fixed width, if desired
     * @param maxHeight     A max fixed height, if desired
     * @return ImageSize.
     */
    public static ImageSize Resize(double currentWidth, double currentHeight, Double width, Double height, Double maxWidth, Double maxHeight) {
        ImageSize tempVar = new ImageSize();
        tempVar.setWidth(currentWidth);
        tempVar.setHeight(currentHeight);
        return Resize(tempVar.clone(), width, height, maxWidth, maxHeight);
    }

    /**
     * Resizes a set of dimensions
     *
     * @param size      The original size object
     * @param width     A new fixed width, if desired
     * @param height    A new fixed height, if desired
     * @param maxWidth  A max fixed width, if desired
     * @param maxHeight A max fixed height, if desired
     * @return A new size object
     */
    public static ImageSize Resize(ImageSize size, Double width, Double height, Double maxWidth, Double maxHeight) {
        double newWidth = size.getWidth();
        double newHeight = size.getHeight();

        if (width != null && height != null) {
            newWidth = width;
            newHeight = height;
        } else if (height != null) {
            newWidth = GetNewWidth(newHeight, newWidth, height);
            newHeight = height;
        } else if (width != null) {
            newHeight = GetNewHeight(newHeight, newWidth, width);
            newWidth = width;
        }

        if (maxHeight != null && maxHeight < newHeight) {
            newWidth = GetNewWidth(newHeight, newWidth, maxHeight);
            newHeight = maxHeight;
        }

        if (maxWidth != null && maxWidth < newWidth) {
            newHeight = GetNewHeight(newHeight, newWidth, maxWidth);
            newWidth = maxWidth;
        }

        ImageSize tempVar = new ImageSize();
        tempVar.setWidth(newWidth);
        tempVar.setHeight(newHeight);
        return tempVar;
    }

    /**
     * Gets the new width.
     *
     * @param currentHeight Height of the current.
     * @param currentWidth  Width of the current.
     * @param newHeight     The new height.
     * @return System.Double.
     */
    private static double GetNewWidth(double currentHeight, double currentWidth, double newHeight) {
        double scaleFactor = newHeight;
        scaleFactor /= currentHeight;
        scaleFactor *= currentWidth;

        return scaleFactor;
    }

    /**
     * Gets the new height.
     *
     * @param currentHeight Height of the current.
     * @param currentWidth  Width of the current.
     * @param newWidth      The new width.
     * @return System.Double.
     */
    private static double GetNewHeight(double currentHeight, double currentWidth, double newWidth) {
        double scaleFactor = newWidth;
        scaleFactor /= currentWidth;
        scaleFactor *= currentHeight;

        return scaleFactor;
    }
}
