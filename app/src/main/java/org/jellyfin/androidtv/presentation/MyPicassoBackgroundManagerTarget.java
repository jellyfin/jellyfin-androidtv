package tv.emby.embyatv.presentation;

import android.graphics.Bitmap;
import android.support.v17.leanback.app.BackgroundManager;

import com.squareup.picasso.Picasso;

import mediabrowser.apiinteraction.EmptyResponse;
import mediabrowser.apiinteraction.Response;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;

/**
 * Created by spam on 9/28/2016.
 */
public class MyPicassoBackgroundManagerTarget extends PicassoBackgroundManagerTarget {

    public MyPicassoBackgroundManagerTarget(BackgroundManager backgroundManager) {
        super(backgroundManager);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        super.onBitmapLoaded(bitmap, loadedFrom);
        TvApp.getApplication().setCurrentBackground(bitmap);
    }
}
