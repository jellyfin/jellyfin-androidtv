package org.jellyfin.androidtv.details;

import android.animation.Animator;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.RandomTransitionGenerator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import mediabrowser.model.dto.BaseItemDto;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.base.BaseActivity;
import org.jellyfin.androidtv.base.IKeyListener;
import org.jellyfin.androidtv.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.playback.MediaManager;
import org.jellyfin.androidtv.presentation.MyRandomeKBGenerator;
import org.jellyfin.androidtv.presentation.PositionableListRowPresenter;
import org.jellyfin.androidtv.util.Utils;

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

    FrameLayout mPopupArea;
    RowsFragment mPopupRowsFragment;
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

        // Inject the RowsFragment in the popup container
        if (getFragmentManager().findFragmentById(R.id.rows_area) == null) {
            mPopupRowsFragment = new RowsFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.rows_area, mPopupRowsFragment).commit();
        } else {
            mPopupRowsFragment = (RowsFragment) getFragmentManager()
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

        registerKeyListener(new IKeyListener() {
            @Override
            public boolean onKeyUp(int key, KeyEvent event) {
                switch (key) {

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
                        if (mPopupPanelVisible) hideThumbPanel(); else showThumbPanel();
                        return true;

                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        return handlePlayKey();

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
        });
    }

    protected boolean handlePlayKey() {
        if (mPopupPanelVisible) {
            if (isPlaying) stop();
            Utils.Beep();
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
            Picasso.with(this)
                    .load(Utils.getPrimaryImageUrl(photo, displayWidth, displayHeight))
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
