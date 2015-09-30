package tv.emby.embyatv.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Spinner;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.entities.DisplayPreferences;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 8/23/2015.
 */
public class AudioDelayPopup {

    final int WIDTH = Utils.convertDpToPixel(TvApp.getApplication(), 240);
    final int HEIGHT = Utils.convertDpToPixel(TvApp.getApplication(), 130);

    PopupWindow mPopup;
    Activity mActivity;
    View mAnchor;
    NumberSpinner mDelaySpinner;

    public AudioDelayPopup(Activity activity, View anchor, ValueChangedListener<Long> listener) {
        mActivity = activity;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.audio_delay_popup, null);
        mPopup = new PopupWindow(layout, WIDTH, HEIGHT);
        mPopup.setFocusable(true);
        mPopup.setOutsideTouchable(true);
        mPopup.setBackgroundDrawable(new BitmapDrawable()); // necessary for popup to dismiss

        mAnchor = anchor;

        mDelaySpinner = (NumberSpinner) layout.findViewById(R.id.numberSpinner);
        mDelaySpinner.setOnChangeListener(listener);

    }

    public boolean isShowing() {
        return (mPopup != null && mPopup.isShowing());
    }

    public void show(long value) {

        mDelaySpinner.setValue(value);
        mPopup.showAtLocation(mAnchor, Gravity.CENTER_VERTICAL, mAnchor.getRight()-60, mAnchor.getTop());

    }

    public void dismiss() {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }
}
