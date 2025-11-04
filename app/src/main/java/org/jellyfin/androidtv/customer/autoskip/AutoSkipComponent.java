package org.jellyfin.androidtv.customer.autoskip;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import androidx.core.util.Consumer;
import androidx.core.util.Supplier;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.danmu.model.AutoSkipModel;
import org.jellyfin.androidtv.util.Utils;

public class AutoSkipComponent {
    private AutoSkipModel autoSkipModel;
    private long tsTime;
    private long teTime;
    private long wsTime;
    private long weTime;
    private boolean touSkipped;
    private boolean weiSkipped;

    private final Runnable touSkipper;
    private final Runnable weiSkipper;
    private final Handler mHandler;

    public AutoSkipComponent(Consumer<Long> skipFunction, Supplier<String> nextTitleGetter, Context context, View view) {
        this.mHandler = new Handler();

        // 片头
        this.touSkipper = () -> {
            if (touSkipped) {
                return;
            }
            touSkipped = true;
            if (autoSkipModel == null) {
                return;
            }

            skipFunction.accept(autoSkipModel.getTeTime() * 1000L);
            Utils.showToast(context, "已为你自动跳过片头");
        };

        // 片尾
        this.weiSkipper = () -> {
            if (weiSkipped) {
                return;
            }
            weiSkipped = true;
            if (autoSkipModel == null) {
                return;
            }

            AutoSkipTipLinearLayout autoSkipTipLinearLayout = view.findViewById(R.id.auto_skip_info);
            String title = nextTitleGetter.get();
            autoSkipTipLinearLayout.showOverplay(title == null ? "" : title, 5000L, () -> skipFunction.accept(weTime), () -> {

            });
        };
    }

    public void seekTo(long start, long end) {
        if (autoSkipModel == null) {
            return;
        }
//        Timber.d("设置 执行跳过操作: start=%s, end=%s", start, end);
        if (teTime >= start && tsTime <= end) {
            setTouSkipped(true);
        }
        if (weTime >= start && wsTime <= end) {
            setWeiSkipped(true);
        }
    }

    public void setTouSkipped(boolean touSkipped) {
        this.touSkipped = touSkipped;
    }

    public void setWeiSkipped(boolean weiSkipped) {
        this.weiSkipped = weiSkipped;
    }

    /**
     * 设置跳过信息
     * @param autoSkipModel 当前跳过信息
     */
    public void setAutoSkipModel(AutoSkipModel autoSkipModel) {
//        Timber.d("autoSkip 设置autoSkipModel - autoSkipModel=%s", JSON.toJSONString(autoSkipModel));
        if (this.autoSkipModel == autoSkipModel) {
            return;
        }

        if (autoSkipModel == null) {
            this.autoSkipModel = null;
            this.touSkipped = true;
            this.weiSkipped = true;
            return;
        }

        this.touSkipped = false;
        this.weiSkipped = false;
        this.autoSkipModel = autoSkipModel;
        // 开始时间+50毫秒
        this.tsTime = this.autoSkipModel.getTsTime() * 1000L + 50L;
        this.teTime = this.autoSkipModel.getTeTime() * 1000L;

        // 片尾开始时间-5秒，提示跳过
        this.wsTime = this.autoSkipModel.getWsTime() * 1000L - 5000L;
        this.weTime = this.autoSkipModel.getWeTime() * 1000L;

        // 没有设置片尾，直接标记跳过
        if (wsTime <= 0) {
            weTime = Long.MAX_VALUE;
            this.weiSkipped = true;
        }

        if (weTime == 0L) {
            weTime = Long.MAX_VALUE;
        }
    }

    /**
     * 自动跳过
     * @param currentPosition 当前时间
     */
    public void autoSkip(long currentPosition) {
        if (autoSkipModel == null) {
            return;
        }
        // 片头
        if (!touSkipped && currentPosition >= tsTime && currentPosition < teTime) {
            mHandler.post(touSkipper);
        }

//        Timber.d("autoSkip 进度测试 - weiSkipped=%s, currentPosition=%s, wsTime=%s, weTime=%s", weiSkipped, currentPosition, wsTime, weTime);
        // 片尾
        if (!weiSkipped && currentPosition >= wsTime && currentPosition < weTime) {
            mHandler.post(weiSkipper);
        }
    }
}
