package tv.mediabrowser.mediabrowsertv.details;

import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.SimpleDateFormat;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.library.PlayAccess;
import tv.mediabrowser.mediabrowsertv.R;
import tv.mediabrowser.mediabrowsertv.TvApp;
import tv.mediabrowser.mediabrowsertv.base.BaseActivity;
import tv.mediabrowser.mediabrowsertv.imagehandling.PicassoBackgroundManagerTarget;
import tv.mediabrowser.mediabrowsertv.ui.ImageButton;
import tv.mediabrowser.mediabrowsertv.util.Utils;

/**
 * Created by Eric on 2/19/2015.
 */
public class FullDetailsActivity extends BaseActivity {

    private ImageView mPoster;
    private TextView mTitle;
    private TextView mButtonHelp;
    private LinearLayout mButtonRow;

    private Target mBackgroundTarget;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    private TvApp mApplication;
    private Handler mRotateHandler = new Handler();
    private Runnable mBackdropLoop;
    private int BACKDROP_ROTATION_INTERVAL = 8000;
    private Typeface roboto;

    private BaseItemDto mBaseItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_details);

        mApplication = TvApp.getApplication();

        mPoster = (ImageView) findViewById(R.id.fdPoster);
        mTitle = (TextView) findViewById(R.id.fdTitle);
        mButtonHelp = (TextView) findViewById(R.id.fdButtonHelp);
        mButtonRow = (LinearLayout) findViewById(R.id.fdButtonRow);

        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mTitle.setTypeface(roboto);
        BackgroundManager backgroundManager = BackgroundManager.getInstance(this);
        backgroundManager.attach(getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mBaseItem = TvApp.getApplication().getSerializer().DeserializeFromString(getIntent().getStringExtra("BaseItem"), BaseItemDto.class);

        setTitle(mBaseItem.getName());
        TextView summary = (TextView)findViewById(R.id.fdSummaryText);
        summary.setTypeface(roboto);
        summary.setText(mBaseItem.getOverview());
        LinearLayout mainInfoRow = (LinearLayout)findViewById(R.id.fdMainInfoRow);

        addCriticInfo(mainInfoRow);
        addDate(mainInfoRow);
        addRatingAndRes(mainInfoRow);
        addMediaDetails(mainInfoRow);
        addButtons(mButtonRow);

        mButtonRow.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) mButtonHelp.setText("");
            }
        });

        // Figure image size
        Double aspect = Utils.getImageAspectRatio(mBaseItem);
        int height = aspect > 1 ? Utils.convertDpToPixel(this, 170) : Utils.convertDpToPixel(this, 300);
        int width = (int)((aspect) * height);
        if (width < 10) width = Utils.convertDpToPixel(this, 150);  //Guard against zero size images causing picasso to barf

        Picasso.with(this)
                .load(Utils.getPrimaryImageUrl(mBaseItem, TvApp.getApplication().getApiClient(),false, false, height))
                .resize(width, height)
                .centerInside()
                .error(getDrawable(R.drawable.video))
                .into(mPoster);

        rotateBackdrops();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRotate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRotate();
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public ImageView getPosterView() {
        return mPoster;
    }

    private void addCriticInfo(LinearLayout layout) {
        int imagesize = Utils.convertDpToPixel(this,18);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imagesize,imagesize);
        imageParams.setMargins(0, 5, 10, 0);
        if (mBaseItem.getCommunityRating() != null) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star);
            star.setLayoutParams(imageParams);
            layout.addView(star);

            TextView amt = new TextView(this);
            amt.setTextSize(16);
            amt.setText(mBaseItem.getCommunityRating().toString()+" ");
            layout.addView(amt);
        }

        if (mBaseItem.getCriticRating() != null) {
            ImageView tomato = new ImageView(this);
            tomato.setLayoutParams(imageParams);
            if (mBaseItem.getCriticRating() > 59) {
                tomato.setImageResource(R.drawable.fresh);
            } else {
                tomato.setImageResource(R.drawable.rotten);
            }

            layout.addView(tomato);
            TextView amt = new TextView(this);
            amt.setTextSize(16);
            amt.setText(mBaseItem.getCriticRating().toString() + "% ");
            layout.addView(amt);

        }
        addSpacer(layout, "    ");
    }

    private void addDate(LinearLayout layout) {
        TextView date = new TextView(this);
        date.setTextSize(16);
        if (mBaseItem.getPremiereDate() != null) {
            date.setText(new SimpleDateFormat("d MMM y").format(mBaseItem.getPremiereDate()));
            layout.addView(date);
            addSpacer(layout, "    ");
        } else if (mBaseItem.getProductionYear() != null) {
            date.setText(mBaseItem.getProductionYear().toString());
            layout.addView(date);
            addSpacer(layout, "    ");
        }
    }

    private void addRatingAndRes(LinearLayout layout) {
        if (mBaseItem.getOfficialRating() != null) {
            addBlockText(layout, mBaseItem.getOfficialRating());
            addSpacer(layout, "  ");
        }
        if (mBaseItem.getMediaStreams() != null && mBaseItem.getMediaStreams().size() > 0 && mBaseItem.getMediaStreams().get(0).getWidth() != null) {
            int width = mBaseItem.getMediaStreams().get(0).getWidth();
            if (width > 1910) {
                addBlockText(layout, "1080");
            } else if (width > 1270) {
                addBlockText(layout, "720");
            } else addBlockText(layout, "SD");

            addSpacer(layout, "  ");
        }
    }

    private void addMediaDetails(LinearLayout layout) {
        MediaStream stream = Utils.GetFirstAudioStream(mBaseItem);

        if (stream != null) {
            if (stream.getCodec() != null) {
                String codec = stream.getCodec().equals("dca") ? "DTS" : stream.getCodec().toUpperCase();
                addBlockText(layout, codec);
                addSpacer(layout, " ");
            }
            if (stream.getChannelLayout() != null) {
                addBlockText(layout, stream.getChannelLayout().toUpperCase());
                addSpacer(layout, "  ");
            }
        }
    }

    private void addBlockText(LinearLayout layout, String text) {
        TextView view = new TextView(this);
        view.setTextSize(12);
        view.setTextColor(Color.BLACK);
        view.setText(" " + text + " ");
        view.setBackgroundResource(R.drawable.gray_gradient);
        layout.addView(view);

    }

    private void addSpacer(LinearLayout layout, String sp) {
        TextView mSpacer = new TextView(this);
        mSpacer.setTextSize(16);
        mSpacer.setText(sp);
        layout.addView(mSpacer);

    }

    private void addButtons(LinearLayout layout) {
        int buttonSize = Utils.convertDpToPixel(mApplication, 35);
        if (mBaseItem.getCanResume()) {
            ImageButton resume = new ImageButton(this, R.drawable.resume, buttonSize, "Resume", mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.showToast(mApplication, "Resume Clicked");
                }
            });
            layout.addView(resume);
        }
        if (mBaseItem.getPlayAccess() == PlayAccess.Full) {
            ImageButton play = new ImageButton(this, R.drawable.play, buttonSize, "Play", mButtonHelp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.showToast(mApplication, "Play Clicked");
                }
            });
            layout.addView(play);

        }
    }


    private void rotateBackdrops() {
        mBackdropLoop = new Runnable() {
            @Override
            public void run() {
                updateBackground(Utils.getBackdropImageUrl(mBaseItem, TvApp.getApplication().getApiClient(), true));
                mRotateHandler.postDelayed(this, BACKDROP_ROTATION_INTERVAL);
            }
        };

        mRotateHandler.postDelayed(mBackdropLoop, BACKDROP_ROTATION_INTERVAL);
    }

    private void stopRotate() {
        if (mRotateHandler != null && mBackdropLoop != null) {
            mRotateHandler.removeCallbacks(mBackdropLoop);
        }
    }

    protected void updateBackground(String url) {
        Picasso.with(this)
                .load(url)
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

}
