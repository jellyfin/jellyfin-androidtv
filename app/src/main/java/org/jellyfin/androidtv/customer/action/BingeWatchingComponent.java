package org.jellyfin.androidtv.customer.action;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.common.DoubleClickListener;
import org.jellyfin.androidtv.danmu.model.AutoSkipModel;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.util.UUID;

public interface BingeWatchingComponent {

    /**
     * 当前播放信息
     * @return 当前播放信息
     */
    BaseItemDto getCurrentlyPlayingItem();

    /**
     * 当前存储对象
     * @return 当前存储信息
     */
    CustomerUserPreferences getCustomerUserPreferences();

    /**
     * 本次更新成功的数据
     * @param autoSkipModel 本次更新成功的数据
     */
    default void updateCurrentAutoSkipModel(AutoSkipModel autoSkipModel) {}

    default AutoSkipModel getAutoSkipModel(BaseItemDto currentlyPlayingItem) {
        return getCustomerUserPreferences().getAutoSkipModel(currentlyPlayingItem);
    }

    default void doClick(Context context, View view) {
        View settingArea = view.getRootView().findViewById(R.id.playbackViewExtraSetting);
        if (settingArea == null) {
            return;
        }
        settingArea.setOnKeyListener(new DoubleClickListener() {
            @Override
            protected boolean match(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return false;
                }

                if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_BUTTON_B && keyCode != KeyEvent.KEYCODE_ESCAPE) {
                    return false;
                }

                // 设置页面
                return View.VISIBLE == settingArea.getVisibility();
            }

            @Override
            protected void doubleClickCallback(View v, int keyCode, KeyEvent event) {
                settingArea.setVisibility(View.GONE);
            }
        });

        Button cancelButton = settingArea.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(v -> settingArea.setVisibility(View.INVISIBLE));

        Button confirmButton = settingArea.findViewById(R.id.confirm);
        confirmButton.setOnClickListener(v -> {
            BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
            if (currentlyPlayingItem == null) {
                return;
            }
            UUID seasonId = currentlyPlayingItem.getSeasonId();
            if (seasonId == null) {
                seasonId = currentlyPlayingItem.getId();
            }

            AutoSkipModel autoSkipModel = new AutoSkipModel();
            autoSkipModel.setId(seasonId.toString());
            autoSkipModel.setTsTime(getTime(settingArea, R.id.piantou_start_mi, R.id.piantou_start_ss));
            autoSkipModel.setTeTime(getTime(settingArea, R.id.piantou_end_mi, R.id.piantou_end_ss));
            autoSkipModel.setWsTime(getTime(settingArea, R.id.pianwei_start_mi, R.id.pianwei_start_ss));
            autoSkipModel.setWeTime(getTime(settingArea, R.id.pianwei_end_mi, R.id.pianwei_end_ss));

            Utils.showToast(context, "设置成功");
            updateCurrentAutoSkipModel(autoSkipModel);
            getCustomerUserPreferences().addAutoSkipModel(autoSkipModel);
            settingArea.setVisibility(View.INVISIBLE);
        });

        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        AutoSkipModel autoSkipModel = getAutoSkipModel(currentlyPlayingItem);
        if (autoSkipModel != null) {
            if (autoSkipModel.getTsTime() > 0) {
                String[] miSs = getMiSs(autoSkipModel.getTsTime());
                ((EditText) settingArea.findViewById(R.id.piantou_start_mi)).setText(miSs[0]);
                ((EditText) settingArea.findViewById(R.id.piantou_start_ss)).setText(miSs[1]);
            }
            if (autoSkipModel.getTeTime() > 0) {
                String[] miSs = getMiSs(autoSkipModel.getTeTime());
                ((EditText) settingArea.findViewById(R.id.piantou_end_mi)).setText(miSs[0]);
                ((EditText) settingArea.findViewById(R.id.piantou_end_ss)).setText(miSs[1]);
            }
            if (autoSkipModel.getWsTime() > 0) {
                String[] miSs = getMiSs(autoSkipModel.getWsTime());
                ((EditText) settingArea.findViewById(R.id.pianwei_start_mi)).setText(miSs[0]);
                ((EditText) settingArea.findViewById(R.id.pianwei_start_ss)).setText(miSs[1]);
            }
            if (autoSkipModel.getWeTime() > 0) {
                String[] miSs = getMiSs(autoSkipModel.getWeTime());
                ((EditText) settingArea.findViewById(R.id.pianwei_end_mi)).setText(miSs[0]);
                ((EditText) settingArea.findViewById(R.id.pianwei_end_ss)).setText(miSs[1]);
            }
        }

        settingArea.setVisibility(View.VISIBLE);
        settingArea.findViewById(R.id.piantou_end_mi).requestFocus();
    }

    default int getTime(View settingArea, int miId, int ssId) {
        String mi = ((EditText) settingArea.findViewById(miId)).getText().toString();
        String ss = ((EditText) settingArea.findViewById(ssId)).getText().toString();

        int time = 0;
        if (!mi.trim().isEmpty()) {
            time += Integer.parseInt(mi.trim()) * 60;
        }

        if (!ss.trim().isEmpty()) {
            time += Integer.parseInt(ss.trim());
        }
        return time;
    }

    default String[] getMiSs(int time) {
        return new String[]{String.format("%02d", time / 60), String.format("%02d", time % 60)};
    }
}
