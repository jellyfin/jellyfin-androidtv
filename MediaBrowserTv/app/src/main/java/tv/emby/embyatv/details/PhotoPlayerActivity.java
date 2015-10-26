package tv.emby.embyatv.details;

import android.animation.Animator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
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

    ImageView[] mainImages = new ImageView[2];
    ImageView nextImage;
    ImageView prevImage;
    int currentImageNdx = 0;
    int nextImageNdx = 1;
    int displayWidth;
    int displayHeight;
    boolean isLoadingNext;
    boolean isLoadingPrev;
    boolean isTransitioning;
    boolean isPlaying;

    Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_player);
        mainImages[0] = (ImageView) findViewById(R.id.mainImage);
        mainImages[1] = (ImageView) findViewById(R.id.mainImage2);
        nextImage = new ImageView(this);
        nextImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        prevImage = new ImageView(this);
        prevImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        handler = new Handler();

        currentPhoto = MediaManager.getCurrentMediaItem().getBaseItem();
        loadImage(currentPhoto, currentImageView());
        loadNext();
        loadPrev();

        registerKeyListener(new IKeyListener() {
            @Override
            public boolean onKeyUp(int key, KeyEvent event) {
                switch (key) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (MediaManager.hasNextMediaItem()) {
                            if (isLoadingNext || isTransitioning) return true; //swallow too fast requests
                            if (isPlaying) {
                                stop();
                                play();
                            } else {
                                next();
                            }
                            return true;
                        }

                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (MediaManager.hasPrevMediaItem()) {
                            if (isLoadingPrev || isTransitioning) return true; //swallow too fast requests
                            if (isPlaying) stop();
                            currentPhoto = MediaManager.prev().getBaseItem();
                            nextImage.setImageDrawable(currentImageView().getDrawable());
                            nextImageView().setImageDrawable(prevImage.getDrawable());
                            transition(750);
                            loadPrev();
                            return true;
                        }

                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (isPlaying) stop(); else play();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        if (!isPlaying) play();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        stop();
                        return true;

                    default:
                        return false;
                }

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) stop();
    }

    private void next() {
        currentPhoto = MediaManager.next().getBaseItem();
        prevImage.setImageDrawable(currentImageView().getDrawable());
        nextImageView().setImageDrawable(nextImage.getDrawable());
        transition(750);
        loadNext();

    }

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (MediaManager.hasNextMediaItem()) {
                next();
                handler.postDelayed(this, 8000);
            }
        }
    };

    private void play() {
        isPlaying = true;
        next();
        handler.postDelayed(playRunnable, 8000);
    }

    private void stop() {
        handler.removeCallbacks(playRunnable);
        isPlaying = false;
    }

    private ImageView currentImageView() { return mainImages[currentImageNdx]; }
    private ImageView nextImageView() { return mainImages[nextImageNdx]; }

    private void loadNext() {
        if (MediaManager.hasNextMediaItem()) loadImage(MediaManager.peekNextMediaItem().getBaseItem(), nextImage);

    }

    private void loadPrev() {
        if (MediaManager.hasPrevMediaItem()) loadImage(MediaManager.peekPrevMediaItem().getBaseItem(), prevImage);

    }

    private void transition(int duration) {
        //transition between current image and the next one
        isTransitioning = true;
        currentImageView().animate().alpha(0f).setDuration(duration-50).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                currentImageView().setAlpha(0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        nextImageView().animate().alpha(1).setDuration(duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                nextImageView().setAlpha(1f);
                currentImageNdx = nextImageNdx;
                nextImageNdx = currentImageNdx == 0 ? 1 : 0;
                TvApp.getApplication().getLogger().Debug("Current ndx: "+currentImageNdx+" next: "+nextImageNdx);
                isTransitioning = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isTransitioning = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
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
