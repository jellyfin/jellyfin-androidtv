package org.jellyfin.androidtv.details;

import android.animation.Animator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.flaviofaria.kenburnsview.KenBurnsView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.presentation.MyRandomeKBGenerator;
import org.jellyfin.androidtv.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.ImageUtils;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import timber.log.Timber;

public class PhotoPlayerActivity extends FragmentActivity {
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

    FrameLayout mPopupArea;
    RowsSupportFragment mPopupRowsFragment;
    ArrayObjectAdapter mPopupRowAdapter;
    ListRow mThumbRow;
    PositionableListRowPresenter mPopupRowPresenter;
    Animation showPopup;
    Animation hidePopup;
    boolean mPopupPanelVisible;

    Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_player);
        mainImages[0] = (KenBurnsView) findViewById(R.id.mainImage);
        mainImages[1] = (KenBurnsView) findViewById(R.id.mainImage2);
        nextImage = new ImageView(this);
        nextImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        prevImage = new ImageView(this);
        prevImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        mPopupArea = (FrameLayout) findViewById(R.id.popupArea);

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

        // Inject the RowsSupportFragment in the popup container
        if (getFragmentManager().findFragmentById(R.id.rows_area) == null) {
            mPopupRowsFragment = new RowsSupportFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.rows_area, mPopupRowsFragment).commit();
        } else {
            mPopupRowsFragment = (RowsSupportFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.rows_area);
        }

        mPopupRowPresenter = new PositionableListRowPresenter();
        mPopupRowAdapter = new ArrayObjectAdapter(mPopupRowPresenter);
        mPopupRowsFragment.setAdapter(mPopupRowAdapter);
        mThumbRow = new ListRow(new HeaderItem(""), MediaManager.getCurrentMediaAdapter());
        mPopupRowAdapter.add(mThumbRow);
        mPopupRowsFragment.setOnItemViewClickedListener(itemViewClickedListener);
        mPopupRowsFragment.setOnItemViewSelectedListener(itemViewSelectedListener);
        setupPopupAnimations();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_B:
                if (mPopupPanelVisible) {
                    hideThumbPanel();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!mPopupPanelVisible && MediaManager.hasNextMediaItem()) {
                    if (isLoadingNext || isTransitioning)
                        return true; //swallow too fast requests
                    if (isPlaying) {
                        stop();
                        play();
                    } else {
                        next(750);
                    }
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!mPopupPanelVisible && MediaManager.hasPrevMediaItem()) {
                    if (isLoadingPrev || isTransitioning)
                        return true; //swallow too fast requests
                    if (isPlaying) stop();
                    currentPhoto = MediaManager.prevMedia().getBaseItem();
                    nextImage.setImageDrawable(currentImageView().getDrawable());
                    nextImageView().setImageDrawable(prevImage.getDrawable());
                    transition(750);
                    loadPrev();
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mPopupPanelVisible) hideThumbPanel();
                else showThumbPanel();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                return handlePlayKey();

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                stop();
                return true;

            default:
                return false;
        }

        return false;
    }

    protected boolean handlePlayKey() {
        if (mPopupPanelVisible) {
            if (isPlaying) stop();
            Utils.beep();
            hideThumbPanel();
            MediaManager.setCurrentMediaPosition(mPopupRowPresenter.getPosition());
            loadImage(MediaManager.getCurrentMediaItem().getBaseItem(), currentImageView());
            nextImageView().setAlpha(0f);
            currentImageView().resume();
            loadNext();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    play();
                }
            }, 8000);

            return true;
        }

        if (isPlaying) stop();
        else play();
        return true;

    }

    protected boolean handleSelectKey() {
        if (mPopupPanelVisible) {
            if (isPlaying) stop();
            hideThumbPanel();
            MediaManager.setCurrentMediaPosition(mPopupRowPresenter.getPosition());
            loadImage(MediaManager.getCurrentMediaItem().getBaseItem(), currentImageView());
            nextImageView().setAlpha(0f);
            loadNext();

            return true;
        }

        return false;

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) stop();
    }

    private void next(int transDuration) {
        currentPhoto = MediaManager.nextMedia().getBaseItem();
        prevImage.setImageDrawable(currentImageView().getDrawable());
        nextImageView().setImageDrawable(nextImage.getDrawable());
        transition(transDuration);
        loadNext();

    }

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (MediaManager.hasNextMediaItem()) {
                next(1800);
                handler.postDelayed(this, 8000);
            } else {
                currentImageView().pause();
                mainImages[0].setKeepScreenOn(false);
            }
        }
    };

    private void play() {
        isPlaying = true;
        mainImages[0].setKeepScreenOn(true);
        currentImageView().resume();
        nextImageView().resume();
        next(1800);
        handler.postDelayed(playRunnable, 8000);
    }

    private void stop() {
        currentImageView().pause();
        nextImageView().pause();
        handler.removeCallbacks(playRunnable);
        mainImages[0].setKeepScreenOn(false);
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

            Glide.with(this)
                    .load(ImageUtils.getPrimaryImageUrl(photo, displayWidth, displayHeight))
                    .override(displayWidth, displayHeight)
                    .centerInside()
                    .error(R.drawable.tile_land_photo)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> drawableTarget, boolean isFirstResource) {
                            if (target == nextImage) isLoadingNext = false;
                            if (target == prevImage) isLoadingPrev = false;
                            Timber.d("Error loading item %s", photo.getName());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> drawableTarget, DataSource dataSource, boolean isFirstResource) {
                            if (target == nextImage) isLoadingNext = false;
                            if (target == prevImage) isLoadingPrev = false;
                            Timber.d("Loaded item %s", photo.getName());
                            if (play){
                                currentImageView().resume();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        play();
                                    }
                                }, 5000);
                            }
                            return false;
                        }
                    })
                    .into(target);
        }
    }

    private OnItemViewClickedListener itemViewClickedListener = new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            handleSelectKey();
        }
    };

    private OnItemViewSelectedListener itemViewSelectedListener = new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (!(item instanceof BaseRowItem) || MediaManager.getCurrentMediaAdapter() == null) return;
            MediaManager.getCurrentMediaAdapter().loadMoreItemsIfNeeded(((BaseRowItem)item).getIndex());
        }
    };

    private void setupPopupAnimations() {
        showPopup = AnimationUtils.loadAnimation(this, R.anim.abc_slide_in_bottom);
        showPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mPopupArea.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        hidePopup = AnimationUtils.loadAnimation(this, R.anim.abc_fade_out);
        hidePopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPopupArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


    }

    private void showThumbPanel() {

        mPopupArea.bringToFront();
        mPopupRowPresenter.setPosition(MediaManager.getCurrentMediaPosition());
        mPopupArea.startAnimation(showPopup);
        mPopupPanelVisible = true;
    }

    private void hideThumbPanel(){
        mPopupArea.startAnimation(hidePopup);
        mPopupPanelVisible = false;
    }


}
