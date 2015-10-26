package tv.emby.embyatv.details;

import android.animation.Animator;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.RandomTransitionGenerator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import mediabrowser.model.dto.BaseItemDto;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.base.IKeyListener;
import tv.emby.embyatv.playback.MediaManager;
import tv.emby.embyatv.presentation.MyRandomeKBGenerator;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 10/22/2015.
 */
public class PhotoPlayerActivity extends BaseActivity {
    BaseItemDto currentPhoto;

    KenBurnsView[] mainImages = new KenBurnsView[2];
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
        mainImages[0] = (KenBurnsView) findViewById(R.id.mainImage);
        mainImages[1] = (KenBurnsView) findViewById(R.id.mainImage2);
        nextImage = new ImageView(this);
        nextImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        prevImage = new ImageView(this);
        prevImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        handler = new Handler();

        currentImageView().setTransitionGenerator(new MyRandomeKBGenerator(9000, new AccelerateDecelerateInterpolator()));
        nextImageView().setTransitionGenerator(new MyRandomeKBGenerator(9000, new AccelerateDecelerateInterpolator()));
        currentImageView().pause();
        nextImageView().pause();

        currentPhoto = MediaManager.getCurrentMediaItem().getBaseItem();
        loadImage(currentPhoto, currentImageView(), getIntent().getBooleanExtra("Play", false));
        loadImage(currentPhoto, nextImageView());
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
                                next(750);
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

    private void next(int transDuration) {
        currentPhoto = MediaManager.next().getBaseItem();
        prevImage.setImageDrawable(currentImageView().getDrawable());
        nextImageView().setImageDrawable(nextImage.getDrawable());
        transition(transDuration);
        loadNext();

    }

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (MediaManager.hasNextMediaItem()) {
                next(1500);
                handler.postDelayed(this, 8000);
            }
        }
    };

    private void play() {
        isPlaying = true;
        currentImageView().resume();
        nextImageView().resume();
        next(1500);
        handler.postDelayed(playRunnable, 8000);
    }

    private void stop() {
        currentImageView().pause();
        nextImageView().pause();
        handler.removeCallbacks(playRunnable);
        isPlaying = false;
    }

    private KenBurnsView currentImageView() { return mainImages[currentImageNdx]; }
    private KenBurnsView nextImageView() { return mainImages[nextImageNdx]; }

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
                //TvApp.getApplication().getLogger().Debug("Current ndx: "+currentImageNdx+" next: "+nextImageNdx);
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
        loadImage(photo, target, false);
    }

    private void loadImage(final BaseItemDto photo, final ImageView target, final boolean play) {
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
                            if (play){
                                currentImageView().resume();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        play();
                                    }
                                }, 5000);
                            }
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
