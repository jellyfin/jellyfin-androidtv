package org.jellyfin.androidtv.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.view.View;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.details.ExpandedTextActivity;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityOptionsCompat;

/**
 * Created by spam on 9/30/2016.
 */

public class ExpandableTextView extends AppCompatTextView {

    private boolean textChanged;
    private boolean isElipsized;

    public ExpandableTextView(Context context) {
        super(context);
        init();
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        final View us = this;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundEffect(SoundEffectConstants.CLICK);
                Intent expanded = new Intent(TvApp.getApplication(), ExpandedTextActivity.class);
                expanded.putExtra("text", getText().toString());
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(TvApp.getApplication().getCurrentActivity(), us, "summary");
                TvApp.getApplication().getCurrentActivity().startActivity(expanded, options.toBundle());
            }
        });

    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        textChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (textChanged) {
            textChanged = false;
            Layout l = getLayout();
            if ( l != null) {
                int lines = l.getLineCount();
                isElipsized = lines > 0 && l.getEllipsisCount(lines - 1) > 0;
                setFocusable(isElipsized);
            } else
                isElipsized = false;

        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focused) {
            setBackgroundResource(R.drawable.txt_focus);
        } else {
            setBackgroundColor(0);
        }
    }

    public boolean isElipsized() {
        return isElipsized;
    }
}
