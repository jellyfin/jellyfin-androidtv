package tv.emby.embyatv.details;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.BaseItemPerson;
import mediabrowser.model.dto.UserItemDataDto;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.PersonType;
import mediabrowser.model.library.PlayAccess;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.base.BaseActivity;
import tv.emby.embyatv.imagehandling.PicassoBackgroundManagerTarget;
import tv.emby.embyatv.playback.PlaybackOverlayActivity;
import tv.emby.embyatv.ui.GenreButton;
import tv.emby.embyatv.ui.ImageButton;
import tv.emby.embyatv.util.InfoLayoutHelper;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 2/19/2015.
 */
public class FullDetailsActivity extends BaseActivity {

    private ImageView mPoster;
    private TextView mTitle;
    private TextView mButtonHelp;
    private TextView mLastPlayedText;
    private TextView mTimeLine;
    private TextView mClock;
    private LinearLayout mButtonRow;
    private ImageButton mResumeButton;

    private int BUTTON_SIZE;

    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    private Calendar mLastUpdated;

    private TvApp mApplication;
    private Handler mLoopHandler = new Handler();
    private Runnable mBackdropLoop;
    private Runnable mClockLoop;
    private int BACKDROP_ROTATION_INTERVAL = 8000;
    private Typeface roboto;

    private BaseItemDto mBaseItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_details);

        mApplication = TvApp.getApplication();
        BUTTON_SIZE = Utils.convertDpToPixel(mApplication, 35);

        mPoster = (ImageView) findViewById(R.id.fdPoster);
        mTitle = (TextView) findViewById(R.id.fdTitle);
        mButtonHelp = (TextView) findViewById(R.id.fdButtonHelp);
        mLastPlayedText = (TextView) findViewById(R.id.fdLastPlayedText);
        mButtonRow = (LinearLayout) findViewById(R.id.fdButtonRow);
        LinearLayout genreRow = (LinearLayout) findViewById(R.id.fdGenreRow);
        mTimeLine = (TextView) findViewById(R.id.fdSummarySubTitle);
        mClock = (TextView) findViewById(R.id.fdClock);

        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mTitle.setTypeface(roboto);
        mLastPlayedText.setTypeface(roboto);
        mClock.setTypeface(roboto);
        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mBaseItem = TvApp.getApplication().getSerializer().DeserializeFromString(getIntent().getStringExtra("BaseItem"), BaseItemDto.class);

        setTitle(mBaseItem.getName());
        if (mBaseItem.getName().length() > 32) {
            // scale down the title so more will fit
            mTitle.setTextSize(32);
        }
        TextView summary = (TextView)findViewById(R.id.fdSummaryText);
        summary.setTypeface(roboto);
        summary.setMovementMethod(new ScrollingMovementMethod());
        summary.setText(mBaseItem.getOverview());
        setSummaryTitles();
        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        InfoLayoutHelper.addInfoRow(this, mBaseItem, mainInfoRow, false);
        addGenres(genreRow);
        addButtons(mButtonRow, BUTTON_SIZE);
        updatePlayedDate();

        mButtonRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) mButtonHelp.setText("");
            }
        });

        updatePoster();

        mButtonRow.requestFocus();

        mLastUpdated = Calendar.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        rotateBackdrops();
        startClock();

        //Update information that may have changed
        if (mApplication.getLastPlayback().after(mLastUpdated)) {
            mApplication.getLogger().Debug("Updating info after playback");
            mApplication.getApiClient().GetItemAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto response) {
                    mBaseItem = response;
                    updatePoster();
                    if ((mResumeButton == null || mResumeButton.getVisibility() == View.GONE) && mBaseItem.getCanResume()) {
                        addResumeButton(mButtonRow, BUTTON_SIZE);
                    }
                    updatePlayedDate();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRotate();
        stopClock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotate();
        stopClock();
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public ImageView getPosterView() {
        return mPoster;
    }

    private void updatePlayedDate() {
        mLastPlayedText.setText(mBaseItem.getUserData() != null && mBaseItem.getUserData().getLastPlayedDate() != null ?
                getString(R.string.lbl_last_played)+ DateUtils.getRelativeTimeSpanString(Utils.convertToLocalDate(mBaseItem.getUserData().getLastPlayedDate()).getTime()).toString()
                : getString(R.string.lbl_never_played));
    }

    private void updatePoster() {
        // Figure image size
        Double aspect = Utils.getImageAspectRatio(mBaseItem, false);
        int height = aspect > 1 ? Utils.convertDpToPixel(this, 170) : Utils.convertDpToPixel(this, 300);
        if (aspect > 1) {
            //Adjust min width of poster area so text doesn't jump over after loading of image
            mPoster.setMinimumWidth(Utils.convertDpToPixel(this, 255));
        }
        int width = (int)((aspect) * height);
        if (width < 10) width = Utils.convertDpToPixel(this, 150);  //Guard against zero size images causing picasso to barf

        Picasso.with(this)
                .load(Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(),false, false, height))
                .skipMemoryCache()
                .resize(width, height)
                .centerInside()
                .error(getResources().getDrawable(R.drawable.video))
                .into(mPoster);

    }

    private void setSummaryTitles() {
        switch (mBaseItem.getType()) {
            case "Person":
                break;
            default:
                TextView topLine = (TextView) findViewById(R.id.fdSummaryTitle);

                BaseItemPerson director = Utils.GetFirstPerson(mBaseItem, PersonType.Director);
                if (director != null) {
                    topLine.setText(getString(R.string.lbl_directed_by)+director.getName());
                }
                setEndTime();
        }
    }

    private void setEndTime() {
        Long runtime = Utils.NullCoalesce(mBaseItem.getRunTimeTicks(), mBaseItem.getOriginalRunTimeTicks());
        if (runtime != null && runtime > 0) {
            long endTimeTicks = System.currentTimeMillis() + runtime / 10000;
            String text = getString(R.string.lbl_runs) + runtime / 600000000 + getString(R.string.lbl_min) + "  " + getString(R.string.lbl_ends) + android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks));
            if (mBaseItem.getCanResume()) {
                endTimeTicks = System.currentTimeMillis() + ((runtime - mBaseItem.getUserData().getPlaybackPositionTicks()) / 10000);
                text += " ("+android.text.format.DateFormat.getTimeFormat(this).format(new Date(endTimeTicks))+getString(R.string.lbl_if_resumed);
            }
            mTimeLine.setText(text);
        }

    }

    private void updateClock() {
        mClock.setText(Utils.getCurrentFormattedTime());
    }

    private void addGenres(LinearLayout layout) {
        if (mBaseItem.getGenres() != null && mBaseItem.getGenres().size() > 0) {
            int i = 0;
            for (String genre : mBaseItem.getGenres()) {
                layout.addView(new GenreButton(this, roboto, 16, genre, mBaseItem.getType()));
            }
        }
    }

    private void addButtons(LinearLayout layout, int buttonSize) {
        if (mBaseItem.getCanResume()) {
            addResumeButton(layout, buttonSize);
        }
        if (mBaseItem.getPlayAccess() == PlayAccess.Full) {
            ImageButton play = new ImageButton(this, R.drawable.play, buttonSize, getString(R.string.lbl_play), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(mBaseItem, 0, false);
                }
            });
            layout.addView(play);
        }
        UserItemDataDto userData = mBaseItem.getUserData();
        if (userData != null) {
            final ImageButton watched = new ImageButton(this, userData.getPlayed() ? R.drawable.redcheck : R.drawable.whitecheck, buttonSize, getString(R.string.lbl_toggle_watched), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final UserItemDataDto data = mBaseItem.getUserData();
                    if (data.getPlayed()) {
                        mApplication.getApiClient().MarkUnplayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
                                mBaseItem.setUserData(response);
                                ((ImageButton)v).setImageResource(R.drawable.whitecheck);
                                //adjust resume
                                if (mResumeButton != null && !mBaseItem.getCanResume()) mResumeButton.setVisibility(View.GONE);
                                //force lists to re-fetch
                                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
                            }
                        });
                    } else {
                        mApplication.getApiClient().MarkPlayedAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), null, new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
                                mBaseItem.setUserData(response);
                                ((ImageButton)v).setImageResource(R.drawable.redcheck);
                                //adjust resume
                                if (mResumeButton != null && !mBaseItem.getCanResume()) mResumeButton.setVisibility(View.GONE);
                                //force lists to re-fetch
                                TvApp.getApplication().setLastPlayback(Calendar.getInstance());
                            }
                        });
                    }
                }
            });
            layout.addView(watched);

            //Favorite
            ImageButton fav = new ImageButton(this, userData.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart, buttonSize, getString(R.string.lbl_toggle_favorite), mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    UserItemDataDto data = mBaseItem.getUserData();
                        mApplication.getApiClient().UpdateFavoriteStatusAsync(mBaseItem.getId(), mApplication.getCurrentUser().getId(), !data.getIsFavorite(), new Response<UserItemDataDto>() {
                            @Override
                            public void onResponse(UserItemDataDto response) {
                                mBaseItem.setUserData(response);
                                ((ImageButton)v).setImageResource(response.getIsFavorite() ? R.drawable.redheart : R.drawable.whiteheart);
                            }
                        });
                }
            });
            layout.addView(fav);
        }

//        if (mBaseItem.getCanDelete()) {
//            final Activity activity = this;
//            ImageButton del = new ImageButton(this, R.drawable.trash, buttonSize, "Delete", mButtonHelp, new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    new AlertDialog.Builder(activity)
//                            .setTitle("Delete")
//                            .setMessage("This will PERMANENTLY DELETE " + mBaseItem.getName() + " from your library.  Are you VERY sure?")
//                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int whichButton) {
//                                    Utils.showToast(activity, "Would delete...");
//                                }
//                            })
//                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    Utils.showToast(activity, "Item NOT Deleted");
//                                }
//                            })
//                            .show();
//
//                }
//            });
//            layout.addView(del);
//        }
    }

    private void addResumeButton(LinearLayout layout, int buttonSize) {
        mResumeButton = new ImageButton(this, R.drawable.resume, buttonSize, getString(R.string.lbl_resume), mButtonHelp, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long pos = mBaseItem.getUserData().getPlaybackPositionTicks() / 10000;
                play(mBaseItem, pos.intValue(), false);
            }
        });
        layout.addView(mResumeButton, 0);
    }

    protected void play(final BaseItemDto item, final int pos, final boolean shuffle) {
        final Activity activity = this;
        Utils.getItemsToPlay(item, pos == 0 && item.getType().equals("Movie"), shuffle, new Response<String[]>() {
            @Override
            public void onResponse(String[] response) {
                Intent intent = new Intent(activity, PlaybackOverlayActivity.class);
                intent.putExtra("Items", response);
                intent.putExtra("Position", pos);
                startActivity(intent);
            }
        });

    }

    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
                mLoopHandler.postDelayed(this, BACKDROP_ROTATION_INTERVAL);
            }
        };

        mLoopHandler.postDelayed(mBackdropLoop, BACKDROP_ROTATION_INTERVAL);
    }

    private void stopRotate() {
        if (mLoopHandler != null && mBackdropLoop != null) {
            mLoopHandler.removeCallbacks(mBackdropLoop);
        }
    }

    private void startClock() {
        updateClock();
        mClockLoop = new Runnable() {
            @Override
            public void run() {
                updateClock();
                setEndTime();
                mLoopHandler.postDelayed(this, 15000);
            }
        };

        mLoopHandler.postDelayed(mClockLoop, 15000);
    }

    private void stopClock() {
        if (mLoopHandler != null && mClockLoop != null) {
            mLoopHandler.removeCallbacks(mClockLoop);
        }
    }

    protected void updateBackground(String url) {
        Picasso.with(this)
                .load(url)
                .skipMemoryCache()
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

}
