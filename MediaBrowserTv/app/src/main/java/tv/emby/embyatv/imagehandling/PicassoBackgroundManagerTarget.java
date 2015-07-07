package tv.emby.embyatv.imagehandling;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.app.BackgroundManager;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Picasso target for updating default_background images
 */
public class PicassoBackgroundManagerTarget implements Target {
    BackgroundManager mBackgroundManager;

    public PicassoBackgroundManagerTarget(BackgroundManager backgroundManager) {
        this.mBackgroundManager = backgroundManager;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        try {
            this.mBackgroundManager.setBitmap(bitmap);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBitmapFailed(Drawable drawable) {
        try {
            this.mBackgroundManager.setDrawable(drawable);

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepareLoad(Drawable drawable) {
        // Do nothing, default_background manager has its own transitions
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PicassoBackgroundManagerTarget that = (PicassoBackgroundManagerTarget) o;

        if (!mBackgroundManager.equals(that.mBackgroundManager))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mBackgroundManager.hashCode();
    }
}
