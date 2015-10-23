package tv.emby.embyatv.details;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.livetv.TvManager;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class PhotoPlayerActivity extends BaseActivity {
    BaseItemDto currentPhoto;

    ImageView mainImage;
    ImageView nextImage;
    ImageView prevImage;
    int displayWidth;
    int displayHeight;
    boolean isLoadingNext;
    boolean isLoadingPrev;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_player);
        mainImage = (ImageView) findViewById(R.id.mainImage);
        nextImage = new ImageView(this);
        nextImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        prevImage = new ImageView(this);
        prevImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        currentPhoto = MediaManager.getCurrentMediaItem().getBaseItem();
        loadImage(currentPhoto, mainImage);
        loadNext();
        loadPrev();

        registerKeyListener(new IKeyListener() {
            @Override
            public boolean onKeyUp(int key, KeyEvent event) {
                switch (key) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (MediaManager.hasNextMediaItem()) {
                            if (isLoadingNext) return true; //swallow too fast requests
                            currentPhoto = MediaManager.next().getBaseItem();
                            prevImage.setImageDrawable(mainImage.getDrawable());
                            mainImage.setImageDrawable(nextImage.getDrawable());
                            loadNext();
                            return true;
                        }

                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (MediaManager.hasPrevMediaItem()) {
                            if (isLoadingPrev) return true; //swallow too fast requests
                            currentPhoto = MediaManager.prev().getBaseItem();
                            nextImage.setImageDrawable(mainImage.getDrawable());
                            mainImage.setImageDrawable(prevImage.getDrawable());
                            loadPrev();
                            return true;
                        }

                    default:
                        return false;
                }

            }
        });
    }

    private void loadNext() {
        if (MediaManager.hasNextMediaItem()) loadImage(MediaManager.peekNextMediaItem().getBaseItem(), nextImage);

    }

    private void loadPrev() {
        if (MediaManager.hasPrevMediaItem()) loadImage(MediaManager.peekPrevMediaItem().getBaseItem(), prevImage);

    }

    private void loadImage(final BaseItemDto photo, final ImageView target) {
        if (photo != null) {
            if (target == nextImage) isLoadingNext = true;
            if (target == prevImage) isLoadingPrev = true;
            Picasso.with(this)
                    .load(Utils.getPrimaryImageUrl(photo, displayWidth))
                    .resize(displayWidth, displayHeight)
                    .centerInside()
                    .skipMemoryCache()
                    .error(R.drawable.photo)
                    .into(target, new Callback() {
                        @Override
                        public void onSuccess() {
                            if (target == nextImage) isLoadingNext = false;
                            if (target == prevImage) isLoadingPrev = false;
                            TvApp.getApplication().getLogger().Debug("Loaded item "+photo.getName());
                        }

                        @Override
                        public void onError() {
                            if (target == nextImage) isLoadingNext = false;
                            if (target == prevImage) isLoadingPrev = false;
                            TvApp.getApplication().getLogger().Debug("Error loading item "+photo.getName());
                        }
                    });
        }
    }
}
