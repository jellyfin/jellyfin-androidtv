package org.jellyfin.androidtv.customer.autoskip;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.jellyfin.androidtv.R;

public class AutoSkipTipLinearLayout extends LinearLayout {
    private final Context context;
    private Handler postHandler;
    private TextView titleView;
    private Button confirmButton;
    private Animation showBottomTipPopup;
    private Animation hideBottomTipPopup;
    private View.OnKeyListener onKeyListener;
    private Runnable canceler;

    private boolean mAutoSkipVisible;

    private boolean hasCancel;


    public AutoSkipTipLinearLayout(Context context) {
        this(context, null);

    }

    public AutoSkipTipLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoSkipTipLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AutoSkipTipLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init(context);
    }

    protected void init(Context context) {
        titleView = findViewById(R.id.bottom_tip_title);
        confirmButton = findViewById(R.id.confirm);
        postHandler = new Handler();
        setupBottomTipAnimations(context);
    }

    /**
     * 显示
     * @param title 显示信息
     * @param time 间隔多少时间跳转
     * @param confirm 确认回调
     * @param canceler 取消回调
     *
     */
    public void showOverplay(String title, long time, Runnable confirm, Runnable canceler) {
        // 参数初始化
        mAutoSkipVisible = true;
        this.hasCancel = false;
        confirmButton.setOnClickListener(v -> {
            hideOverlay();
            confirm.run();
        });
        this.canceler = canceler;

        // 定时刷新
        long nextTime = System.currentTimeMillis() + time;
        Runnable autoRefresh = new Runnable() {
            @Override
            public void run() {
                if (mAutoSkipVisible && !hasCancel) {
                    String showString = String.format(context.getString(R.string.auto_skip_tip), title, String.valueOf((nextTime - System.currentTimeMillis()) / 1000));
                    SpannableString spannableString = new SpannableString(showString);
                    int startIndex = showString.indexOf(title);
                    spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), startIndex, startIndex + title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    titleView.setText(spannableString);
                    postHandler.postDelayed(this, 300L);
                }
            }
        };
        postHandler.post(autoRefresh);

        // 定时关闭
        postHandler.postDelayed(() -> {
            if (mAutoSkipVisible && !hasCancel) {
                hideOverlay();
                confirm.run();
            }
        }, time);

        if (showBottomTipPopup != null) {
            this.startAnimation(showBottomTipPopup);
        }
    }

    /**
     * 隐藏
     */
    public void hideOverlay() {
        mAutoSkipVisible = false;
        if (hideBottomTipPopup != null) {
            this.startAnimation(hideBottomTipPopup);
        }
    }

    protected void setupBottomTipAnimations(Context context) {
        showBottomTipPopup = AnimationUtils.loadAnimation(context, androidx.appcompat.R.anim.abc_slide_in_bottom);
        showBottomTipPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                AutoSkipTipLinearLayout.this.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AutoSkipTipLinearLayout.this.confirmButton.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        hideBottomTipPopup = AnimationUtils.loadAnimation(context, androidx.appcompat.R.anim.abc_fade_out);
        hideBottomTipPopup.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                AutoSkipTipLinearLayout.this.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mAutoSkipVisible) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
            hasCancel = true;
            hideOverlay();
            if (canceler != null) {
                canceler.run();
            }
            return true;
        }
        return false;
    }
}
