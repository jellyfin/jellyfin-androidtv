package tv.emby.embyatv.details;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.itemhandling.BaseRowItem;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class PhotoPlayerActivity extends BaseActivity {
    BaseItemDto currentPhoto;

    ImageView mainImage;
    ImageView nextImage;
    int displayWidth;
    int displayHeight;
    boolean isLoading;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_player);
        mainImage = (ImageView) findViewById(R.id.mainImage);
        nextImage = new ImageView(this);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        currentPhoto = MediaManager.getCurrentMediaItem().getBaseItem();
        loadImage(currentPhoto, mainImage);
        loadNext();

        registerKeyListener(new IKeyListener() {
            @Override
            public boolean onKeyUp(int key, KeyEvent event) {
                switch (key) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (MediaManager.hasNextMediaItem()) {
                            if (isLoading) return true; //swallow too fast requests
                            currentPhoto = MediaManager.next().getBaseItem();
                            TransitionDrawable td = new TransitionDrawable(new Drawable[] {mainImage.getDrawable(), nextImage.getDrawable()});
                            mainImage.setImageDrawable(td);
                            td.startTransition(150);
                            loadNext();
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

    private void loadImage(BaseItemDto photo, ImageView target) {
        if (photo != null) {
            isLoading = true;
            Picasso.with(this)
                    .load(Utils.getPrimaryImageUrl(photo, displayWidth))
                    .resize(displayWidth, displayHeight)
                    .centerInside()
                    .skipMemoryCache()
                    .error(R.drawable.photo)
                    .into(target, new Callback() {
                        @Override
                        public void onSuccess() {
                            isLoading = false;
                        }

                        @Override
                        public void onError() {
                            isLoading = false;
                        }
                    });
        }
    }
}
