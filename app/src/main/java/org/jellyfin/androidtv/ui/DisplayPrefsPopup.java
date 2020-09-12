package org.jellyfin.androidtv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Spinner;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.util.Utils;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.entities.DisplayPreferences;

public class DisplayPrefsPopup {

    final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 350);
    final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 310);

    PopupWindow mPopup;
    Activity mActivity;
    View mAnchor;
    DisplayPreferences mPrefs;
    Spinner mImageSize;
    Spinner mImageType;
    Spinner mInitialView;
    View mDefaultViewLayout;

    Boolean mChanged = false;

    public DisplayPrefsPopup(Activity activity, View anchor, boolean allowViewDefault, final Response<Boolean> response) {
        mActivity = activity;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.display_prefs, null);
        mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // necessary for popup to dismiss
        mPopup.setAnimationStyle(R.style.PopupSlideInRight);

        mAnchor = anchor;

        mImageSize = (Spinner) layout.findViewById(R.id.posterSize);
        ArrayAdapter<String> displaySizes = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item);
        {
            displaySizes.add("Auto");
            displaySizes.add("Small");
            displaySizes.add("Medium");
            displaySizes.add("Large");
        }
        mImageSize.setAdapter(displaySizes);
        mImageSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPrefs.getCustomPrefs().put("PosterSize", Integer.toString(position));
                mChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mImageType = (Spinner) layout.findViewById(R.id.imageType);
        ArrayAdapter<String> imageTypes = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item);
        {
            imageTypes.add("Default");
            imageTypes.add("Thumb");
            imageTypes.add("Banner");
        }
        mImageType.setAdapter(imageTypes);
        mImageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPrefs.getCustomPrefs().put("ImageType", Integer.toString(position));
                mChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mInitialView = (Spinner) layout.findViewById(R.id.initialView);
        if (allowViewDefault) {
            ArrayAdapter<String> viewTypes = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item);
            {
                viewTypes.add("Smart Screen");
                viewTypes.add("Grid View");
            }
            mInitialView.setAdapter(viewTypes);
            mInitialView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mPrefs.getCustomPrefs().put("DefaultView", Integer.toString(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        } else {
            mInitialView.setVisibility(View.GONE);
            layout.findViewById(R.id.defaultViewLbl).setVisibility(View.GONE);
        }

        Button done = (Button) layout.findViewById(R.id.btnDone);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                response.onResponse(mChanged);
                dismiss();
            }
        });

        mDefaultViewLayout = layout.findViewById(R.id.defaultViewLayout);

    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(DisplayPreferences prefs, String collectionType) {

        mPrefs = prefs;
        mChanged = false;
        if (collectionType == null) collectionType = "";
        mImageSize.setSelection(Integer.parseInt(Utils.getSafeValue(prefs.getCustomPrefs().get("PosterSize"), "0")));
        mImageType.setSelection(Integer.parseInt(Utils.getSafeValue(prefs.getCustomPrefs().get("ImageType"), "0")));
        switch (collectionType) {
            case "movies":
            case "tvshows":
            case "music":
                mDefaultViewLayout.setVisibility(View.VISIBLE);
                mInitialView.setSelection(Integer.parseInt(Utils.getSafeValue(prefs.getCustomPrefs().get("DefaultView"), "0")));
                break;
            default:
                mDefaultViewLayout.setVisibility(View.GONE);
        }

        mPopup.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, mAnchor.getRight() - 40, mAnchor.getTop());

    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }
}
