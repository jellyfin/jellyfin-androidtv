package org.jellyfin.androidtv.customer.action;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Function;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.common.ViewNavigationUtils;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import timber.log.Timber;

public interface DanmuSettingActionComponent {
    Object[][] SPEED_VALUES = new Object[][]{
            {"慢", "正常", "快"},
            {1.3f, 1.0f, 0.7f}
    };

    /**
     * 描边属性
     */
    Map<Integer, String> FONT_STYLE = new LinkedHashMap<Integer, String>() {
        {
            put(IDisplayer.DANMAKU_STYLE_DEFAULT, "自动");
            put(IDisplayer.DANMAKU_STYLE_NONE, "无");
            put(IDisplayer.DANMAKU_STYLE_SHADOW, "阴影");
            put(IDisplayer.DANMAKU_STYLE_STROKEN, "描边");
            put(IDisplayer.DANMAKU_STYLE_PROJECTION, "投影");
        }
    };

    /**
     * 当前存储对象
     * @return 当前存储信息
     */
    CustomerUserPreferences getCustomerUserPreferences();

    /**
     * 当前播放信息
     * @return 当前播放信息
     */
    BaseItemDto getCurrentlyPlayingItem();

    DanmakuContext getDanmakuContext();

    IDanmakuView getDanmakuView();

    default void doClick(@NonNull Context context, @NonNull View view) {
        View danmuSetting = view.getRootView().findViewById(R.id.danmu_setting_info);
        if (danmuSetting == null) {
            return;
        }

        CustomerUserPreferences customerUserPreferences = getCustomerUserPreferences();

        // ButtonsNavigation
        List<View> views = new ArrayList<>();

        // 速度
        SeekBar speed = danmuSetting.findViewById(R.id.danmu_setting_speed);
        TextView speedValue = danmuSetting.findViewById(R.id.danmu_setting_speed_value);
        float originDanmuSpeed = customerUserPreferences.getDanmuSpeed();
        int originProgress = (int) (originDanmuSpeed * 10 - 5);
        speed.setProgress(originProgress);
        Function<Integer, String> speedValueGetter = integer -> {
            String format = String.format("%.1f", (integer + 5) / 10.0f);
            if (BuildConfig.DEBUG) {
                Timber.d("数据库值=%s, 原始值=%s, 计算后值=%s", originDanmuSpeed, integer, format);
            }
            return format;
        };
        speedValue.setText(speedValueGetter.apply(originProgress));
        speed.setOnSeekBarChangeListener(new TextSeekBarListener(speedValue, speedValueGetter));
        views.add(speed);

        // 弹幕行数
        SeekBar row = danmuSetting.findViewById(R.id.danmu_setting_row);
        TextView rowValue = danmuSetting.findViewById(R.id.danmu_setting_row_value);
        Function<Integer, String> rowValueGetter = integer -> String.valueOf(integer + 3);
        int originDanmuRow = customerUserPreferences.getDanmuRows();
        int originDanmuRowProgress = originDanmuRow - 3;
        row.setProgress(originDanmuRowProgress);
        rowValue.setText(rowValueGetter.apply(originDanmuRowProgress));
        row.setOnSeekBarChangeListener(new TextSeekBarListener(rowValue, rowValueGetter));
        views.add(row);

        // 字体大小
        SeekBar fontSize = danmuSetting.findViewById(R.id.danmu_setting_font_size);
        TextView fontSizeValue = danmuSetting.findViewById(R.id.danmu_setting_font_size_value);
        Function<Integer, String> fontSizeValueGetter = integer -> String.format("%.1f", (integer + 5) / 10.0f);
        float originDanmuFontSize = customerUserPreferences.getDanmuFontSize();
        int originFontSizeProgress = (int) (originDanmuFontSize * 10 - 5);
        fontSize.setProgress(originFontSizeProgress);
        fontSizeValue.setText(fontSizeValueGetter.apply(originFontSizeProgress));
        fontSize.setOnSeekBarChangeListener(new TextSeekBarListener(fontSizeValue, fontSizeValueGetter));
        views.add(fontSize);

        // 弹幕偏移
        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        UUID seasonId = currentlyPlayingItem == null ? null : currentlyPlayingItem.getSeasonId();
        EditText offsetTimeView = danmuSetting.findViewById(R.id.danmu_setting_offset_time);
        int tempDanmuOffset = 0;
        if (seasonId != null) {
            int seasonDanmuOffset = customerUserPreferences.getSeasonDanmuOffset(seasonId.toString());
            if (seasonDanmuOffset != 0) {
                tempDanmuOffset = seasonDanmuOffset;
                offsetTimeView.setText(String.valueOf(tempDanmuOffset));
            }
        }
        views.add(offsetTimeView);
        int originDanmuOffset = tempDanmuOffset;
        ViewNavigationUtils.viewNextPre(fontSize, offsetTimeView, true, false);

        // 描边
        int originDanmuStyle = customerUserPreferences.getDanmuStyle();
        int[] newDanmuStyle = new int[]{originDanmuStyle};
        RadioGroup fontStyleGroup = danmuSetting.findViewById(R.id.danmu_setting_font_style);
        fontStyleGroup.removeAllViews();
        RadioGroup.OnCheckedChangeListener onCheckedChangeListener = (group, checkedId) -> {
            RadioButton checkedRadioButton = group.findViewById(checkedId);
            if (checkedRadioButton == null) {
                return;
            }
            Object tag = checkedRadioButton.getTag();
            newDanmuStyle[0] = (int) tag;
        };
        fontStyleGroup.setOnClickListener(null);
        List<View> fontStyleViews = new ArrayList<>(FONT_STYLE.size());
        for (Map.Entry<Integer, String> styleEntry : FONT_STYLE.entrySet()) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(styleEntry.getValue());
            radioButton.setTag(styleEntry.getKey());
            radioButton.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            fontStyleGroup.addView(radioButton);
            radioButton.setChecked(false);
            if (originDanmuStyle == styleEntry.getKey()) {
                radioButton.setChecked(true);
                views.add(radioButton);
            }
            fontStyleViews.add(radioButton);
        }
        fontStyleGroup.setOnCheckedChangeListener(onCheckedChangeListener);
        int styleIndex = views.size() - 1;

        // fps加入导航
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch fps = danmuSetting.findViewById(R.id.danmu_setting_fps);
        fps.setChecked(customerUserPreferences.isDanmuFps());
        views.add(fps);

        Button confirm = danmuSetting.findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> {
            DanmakuContext danmakuContext = getDanmakuContext();
            IDanmakuView danmakuView = getDanmakuView();

            /*
             * 0.5 - 2.0
             */
            float danmuSpeed = Float.parseFloat(String.valueOf(speedValue.getText()));
            if (danmuSpeed != originDanmuSpeed) {
                customerUserPreferences.setDanmuSpeed(danmuSpeed);
                danmakuContext.setScrollSpeedFactor(danmuSpeed);
            }

            /*
             * 字体大小缩放
             */
            float danmuFontSize = Float.parseFloat(String.valueOf(fontSizeValue.getText()));
            if (danmuFontSize != originDanmuFontSize) {
                customerUserPreferences.setDanmuFontSize(danmuFontSize);
                danmakuContext.setScaleTextSize(danmuFontSize);
            }

            /*
             * 弹幕行数
             */
            int danmuRow = Integer.parseInt(String.valueOf(rowValue.getText()));
            if (danmuRow != originDanmuRow) {
                customerUserPreferences.setDanmuRows(danmuRow);
                HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
                maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, danmuRow);
                danmakuContext.setMaximumLines(maxLinesPair);
            }

            /*
             * 弹幕偏移
             */
            String offsetTimeStr = String.valueOf(offsetTimeView.getText());
            int danmuOffset = 0;
            if (!offsetTimeStr.isEmpty()) {
                danmuOffset = Integer.parseInt(offsetTimeStr);
            }
            if (danmuOffset != originDanmuOffset && seasonId != null) {
                customerUserPreferences.putSeasonDanmuOffset(seasonId.toString(), danmuOffset);
                danmakuView.setOffsetTime(danmuOffset);
            }

            /*
             * 弹幕描边
             */
            if (newDanmuStyle[0] != originDanmuStyle) {
                customerUserPreferences.setDanmuStyle(newDanmuStyle[0]);
                danmakuContext.setDanmakuStyle(newDanmuStyle[0], 3);
            }

            /*
             * FPS
             */
            boolean checked = fps.isChecked();
            if (checked != customerUserPreferences.isDanmuFps()) {
                customerUserPreferences.setDanmuFps(checked);
                danmakuView.showFPS(checked);
            }
            danmuSetting.setVisibility(View.GONE);
        });
        views.add(confirm);

        Button cancel = danmuSetting.findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> danmuSetting.setVisibility(View.GONE));

        // 清除全部按钮导航逻辑
        views.forEach(ViewNavigationUtils::clearAllNavigation);

        // 确认按钮导航
        ViewNavigationUtils.viewPreNext(confirm, cancel, true, true);
        ViewNavigationUtils.viewPreNext(cancel, confirm, true, true);
        ViewNavigationUtils.viewIndexNext(views, cancel, views.size() - 1);

        // 设置字体导航
        ViewNavigationUtils.listViewsNavigation(fontStyleViews, true);
        for (View tempView : fontStyleViews) {
            ViewNavigationUtils.viewIndexNext(views, tempView, styleIndex);
        }
        // 设置整体导航
        ViewNavigationUtils.listViewsNavigation(views, false);

        speed.requestFocus();
        danmuSetting.setVisibility(View.VISIBLE);
    }
}
