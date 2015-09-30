package tv.emby.embyatv.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import tv.emby.embyatv.R;

public class JumpList extends LinearLayout {

    private CharSelectedListener mCharSelectedListener;

    public JumpList(Context context, CharSelectedListener listener) {
        super(context);
        mCharSelectedListener = listener;
        init(context);
    }

    public JumpList(Context context) {
        super(context);
        init(context);
    }

    public JumpList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        removeAllViews();

        //First a '#' entry
        addView(new TextButton(context, "#", 12, new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCharSelectedListener != null) mCharSelectedListener.onCharSelected("#");
            }
        }));

        for (final Character character : getResources().getString(R.string.byletter_letters).toCharArray()) {
            addView(new TextButton(context, character.toString(), 12, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextButton us = (TextButton) v;
                    if (mCharSelectedListener != null) mCharSelectedListener.onCharSelected(us.getText().toString());
                }
            }));
        }
    }

    public void setFocus(String text) {
        if (text == null) return;

        for (int i = 0; i < getChildCount(); i++) {
            TextButton button = (TextButton) getChildAt(i);
            if (text.equals(button.getText())) {
                button.requestFocus();
                return;
            }
        }
    }
}
