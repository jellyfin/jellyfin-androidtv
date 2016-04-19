package tv.emby.embyatv.integration;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import tv.emby.embyatv.R;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 3/1/2015.
 */
public class RecommendationBuilder {
    private static final String TAG = "RecommendationBuilder";

    private Context mContext;

    private int mId;
    private int mPriority;
    private int mSmallIcon;
    private String mTitle;
    private String mDescription;
    private Bitmap mBitmap;
    private String mBackgroundUri;
    private PendingIntent mIntent;

    public RecommendationBuilder() {
    }

    public RecommendationBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    public RecommendationBuilder setId(int id) {
        mId = id;
        return this;
    }

    public RecommendationBuilder setPriority(int priority) {
        mPriority = priority;
        return this;
    }

    public RecommendationBuilder setTitle(String title) {
        mTitle = title;
        return this;
    }

    public RecommendationBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    public RecommendationBuilder setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        return this;
    }

    public RecommendationBuilder setBackground(String uri) {
        mBackgroundUri = uri;
        return this;
    }

    public RecommendationBuilder setIntent(PendingIntent intent) {
        mIntent = intent;
        return this;
    }

    public RecommendationBuilder setSmallIcon(int resourceId) {
        mSmallIcon = resourceId;
        return this;
    }

    public Notification build() {

        Log.d(TAG, "Building notification - " + this.toString());

        Bundle extras = new Bundle();
        if (mBackgroundUri != null) {
            try {
                Log.d(TAG, "Background - " + Uri.parse(RecommendationContentProvider.CONTENT_URI + URLEncoder.encode(mBackgroundUri, "UTF-8") + "/0").toString());
                extras.putString(Notification.EXTRA_BACKGROUND_IMAGE_URI, Uri.parse(RecommendationContentProvider.CONTENT_URI + URLEncoder.encode(mBackgroundUri, "UTF-8")).toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        Notification notification = new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(mTitle)
                        .setContentText(mDescription)
                        .setPriority(mPriority)
                        .setLocalOnly(true)
                        .setOngoing(true)
                        .setColor(Utils.getBrandColor())
                        .setCategory(Notification.CATEGORY_RECOMMENDATION)
                        .setLargeIcon(mBitmap)
                        .setSmallIcon(mSmallIcon)
                        .setContentIntent(mIntent)
                        .setExtras(extras))
                .build();

        return notification;
    }

    @Override
    public String toString() {
        return "RecommendationBuilder{" +
                ", mId=" + mId +
                ", mPriority=" + mPriority +
                ", mSmallIcon=" + mSmallIcon +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mBitmap='" + mBitmap + '\'' +
                ", mBackgroundUri='" + mBackgroundUri + '\'' +
                ", mIntent=" + mIntent +
                '}';
    }}
