package org.jellyfin.androidtv.customer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.alibaba.fastjson.JSON;

import org.jellyfin.androidtv.BuildConfig;
import org.jellyfin.androidtv.danmu.model.AutoSkipModel;
import org.jellyfin.preference.store.SharedPreferenceStore;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import master.flame.danmaku.danmaku.model.IDisplayer;
import timber.log.Timber;

public class CustomerUserPreferences extends SharedPreferenceStore {
    private static final long AUTO_SKIP_EXPIRE_TIME = 6 * 30 * 24 * 3600 * 1000L;

    private static final String VIDEO_SPEED_KEY = "video_speed";
    private static final String DAN_MU_CONTROLLER = "danmu_controller";
    private static final String DAN_MU_SPEED = "danmu_speed";
    private static final String DAN_MU_FONT_SIZE = "danmu_f_size";
    private static final String DAN_MU_ROWS = "danmu_row";
    private static final String DAN_MU_STYLE= "dm_style";
    private static final String DAN_MU_OFFSET_TIME= "dm_o_t";
    public static final String AUTO_SKIP_TIMES = "autoSkipTime";

    private float videoSpeed;
    private int danmuRows;
    private float danmuSpeed;
    private float danmuFontSize;
    private boolean danmuFps;
    private boolean danmuController;
    private int danmuStyle;
    private Map<String, AutoSkipModel> itemAutoSkipTimes;

    private com.alibaba.fastjson.JSONObject seasonDanmuOffsetTimes;

    public CustomerUserPreferences(@NonNull Context context) {
        super(PreferenceManager.getDefaultSharedPreferences(context));

        videoSpeed = getFloat(VIDEO_SPEED_KEY, 1.0f);
        danmuStyle = getInt(DAN_MU_STYLE, IDisplayer.DANMAKU_STYLE_STROKEN);
        danmuRows = getInt(DAN_MU_ROWS, 5);
        danmuController = getBool(DAN_MU_CONTROLLER, true);
        danmuSpeed = getFloat(DAN_MU_SPEED, 1.0f);
        danmuFontSize = getFloat(DAN_MU_FONT_SIZE, 1.0f);

        try {
            seasonDanmuOffsetTimes = JSON.parseObject(getString(DAN_MU_OFFSET_TIME, "{}"));
        } catch (Exception e) {
            Timber.e(e, "加载弹幕偏移数据失败");
            this.seasonDanmuOffsetTimes = new com.alibaba.fastjson.JSONObject();
            updateDanmuOffsetTime();
        }

        try {
            JSONArray jsonArray = new JSONArray(getString(AUTO_SKIP_TIMES, "[]"));
            Map<String, AutoSkipModel> autoSkipModelHashMap = new HashMap<>();
            long expireTime = System.currentTimeMillis() - AUTO_SKIP_EXPIRE_TIME;
            boolean needDelete = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject autoSkipJSON = jsonArray.getJSONObject(i);
                long cTime = autoSkipJSON.getLong("cTime");
                if (cTime < expireTime || autoSkipJSON.isNull("id")) {
                    needDelete = true;
                    continue;
                }

                AutoSkipModel autoSkipModel = new AutoSkipModel();
                autoSkipModel.setTsTime(autoSkipJSON.getInt("tsTime"));
                autoSkipModel.setTeTime(autoSkipJSON.getInt("teTime"));
                autoSkipModel.setWsTime(autoSkipJSON.getInt("wsTime"));
                autoSkipModel.setWeTime(autoSkipJSON.getInt("weTime"));
                autoSkipModel.setcTime(autoSkipJSON.getLong("cTime"));
                autoSkipModel.setId(autoSkipJSON.getString("id"));
                autoSkipModelHashMap.put(autoSkipModel.getId(), autoSkipModel);
            }

            this.itemAutoSkipTimes = autoSkipModelHashMap;
            if (needDelete) {
                updateAutoSkipModels();
            }
        } catch (Exception e) {
            Timber.e(e, "加载片头片尾缓存记录失败");
            this.itemAutoSkipTimes = new HashMap<>();
        }

        if (BuildConfig.DEBUG) {
            Timber.d("数据库值 加载配置数据=%s", JSON.toJSONString(this));
        }
    }

    public float getDanmuSpeed() {
        return danmuSpeed;
    }

    public void setDanmuSpeed(float danmuSpeed) {
        this.danmuSpeed = danmuSpeed;
        if (BuildConfig.DEBUG) {
            Timber.d("数据库值 setDanmuSpeed 保存数据库=%s", danmuSpeed);
        }
        setFloat(DAN_MU_SPEED, danmuSpeed);
    }

    public boolean isDanmuFps() {
        return danmuFps;
    }

    public void setDanmuFps(boolean danmuFps) {
        this.danmuFps = danmuFps;
    }

    public float getVideoSpeed() {
        return videoSpeed;
    }

    public void setVideoSpeed(float videoSpeed) {
        if (videoSpeed <= 0) {
            return;
        }

        setFloat(VIDEO_SPEED_KEY, videoSpeed);
        this.videoSpeed = videoSpeed;
    }

    public float getDanmuFontSize() {
        return danmuFontSize;
    }

    public void setDanmuFontSize(float danmuFontSize) {
        setFloat(DAN_MU_FONT_SIZE, danmuFontSize);
        this.danmuFontSize = danmuFontSize;
    }

    public int getDanmuRows() {
        return danmuRows;
    }

    public int getDanmuStyle() {
        return danmuStyle;
    }

    public void setDanmuStyle(int danmuStyle) {
        setInt(DAN_MU_STYLE, danmuStyle);
        this.danmuStyle = danmuStyle;
    }

    public void setDanmuRows(int danmuRows) {
        setInt(DAN_MU_ROWS, danmuRows);
        this.danmuRows = danmuRows;
    }

    public boolean isDanmuController() {
        return danmuController;
    }

    public void setDanmuController(boolean danmuController) {
        this.danmuController = danmuController;
        setBool(DAN_MU_CONTROLLER, danmuController);
    }

    public AutoSkipModel getAutoSkipModel(String id) {
        return itemAutoSkipTimes.get(id);
    }

    public AutoSkipModel getAutoSkipModel(BaseItemDto baseItemDto) {
        UUID seasonId = baseItemDto.getSeasonId();
        if (seasonId == null) {
            seasonId = baseItemDto.getId();
        }
        return itemAutoSkipTimes.get(seasonId.toString());
    }

    public void addAutoSkipModel(AutoSkipModel autoSkipModel) {
        itemAutoSkipTimes.put(autoSkipModel.getId(), autoSkipModel);
        updateAutoSkipModels();
    }


    public int getSeasonDanmuOffset(BaseItemDto baseItemDto) {
        UUID seasonId = baseItemDto.getSeasonId();
        if (seasonId == null) {
            return 0;
        }
        return getSeasonDanmuOffset(seasonId.toString());
    }

    public int getSeasonDanmuOffset(String seasonId) {
        return seasonDanmuOffsetTimes.getIntValue(seasonId);
    }

    public void putSeasonDanmuOffset(String seasonId, Integer offsetTime) {
        if (offsetTime == null || offsetTime == 0) {
            seasonDanmuOffsetTimes.remove(seasonId);
            return;
        }
        seasonDanmuOffsetTimes.put(seasonId, offsetTime);
        updateDanmuOffsetTime();
    }

    private void updateDanmuOffsetTime() {
        if (seasonDanmuOffsetTimes != null) {
            setString(DAN_MU_OFFSET_TIME, seasonDanmuOffsetTimes.toJSONString());
        }
    }

    private void updateAutoSkipModels() {
        setString(AUTO_SKIP_TIMES, JSON.toJSONString(itemAutoSkipTimes.values()));
    }
}
