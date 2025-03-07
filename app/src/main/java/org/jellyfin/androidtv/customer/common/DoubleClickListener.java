package org.jellyfin.androidtv.customer.common;

import android.view.KeyEvent;
import android.view.View;

public abstract class DoubleClickListener implements View.OnKeyListener {
    private long lastClickBackTime;

    protected long getDoubleClickTime() {
        return 3000L;
    }

    /**
     * 准入条件
     * @param v 当前view
     * @param keyCode 当前按键
     * @param event 当前事件
     * @return 是否满足准入
     */
    protected abstract boolean match(View v, int keyCode, KeyEvent event);

    /**
     * 双击成功回调
     * @param v 当前view
     * @param keyCode 当前按键
     * @param event 当前事件
     */
    protected abstract void doubleClickCallback(View v, int keyCode, KeyEvent event);

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (match(v, keyCode, event)) {
            boolean isDoubleClick = System.currentTimeMillis() - lastClickBackTime < getDoubleClickTime();
            if (isDoubleClick) {
                lastClickBackTime = 0L;
                doubleClickCallback(v, keyCode, event);
            } else {
                lastClickBackTime = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }
}
