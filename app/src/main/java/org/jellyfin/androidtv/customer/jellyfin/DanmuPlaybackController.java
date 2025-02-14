package org.jellyfin.androidtv.customer.jellyfin;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jellyfin.androidtv.customer.ByteReadChannelInputStream;
import org.jellyfin.androidtv.customer.CustomerUserPreferences;
import org.jellyfin.androidtv.customer.common.CustomerCommonUtils;
import org.jellyfin.androidtv.customer.danmu.BiliDanmukuParser;
import org.jellyfin.androidtv.customer.danmu.DanamakuAdapter;
import org.jellyfin.androidtv.danmu.api.DanmuApi;
import org.jellyfin.androidtv.data.compat.StreamInfo;
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.VideoManager;
import org.jellyfin.sdk.api.client.Response;
import org.jellyfin.sdk.api.client.exception.InvalidStatusException;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.koin.java.KoinJavaComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import io.ktor.utils.io.ByteReadChannel;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;

public class DanmuPlaybackController extends PlaybackController {
    private static final Logger log = LoggerFactory.getLogger(DanmuPlaybackController.class);
    private DanmuApi danmuApi;
    private CustomerUserPreferences customerUserPreferences;

    private BaseDanmakuParser mParser;//解析器对象
    private IDanmakuView mDanmakuView;//弹幕view
    private DanmakuContext mDanmakuContext;

    private volatile long mDanmakuStartSeekPosition = -1;
    private float videoSpeed = 1.0f;
    private boolean mDanmaKuShow;

    private BaseItemDto danmuItem;

    public DanmuPlaybackController(List<BaseItemDto> items, CustomPlaybackOverlayFragment fragment) {
        super(items, fragment);
    }

    public DanmuPlaybackController(List<BaseItemDto> items, CustomPlaybackOverlayFragment fragment, int startIndex) {
        super(items, fragment, startIndex);
        customerUserPreferences = KoinJavaComponent.get(CustomerUserPreferences.class);
    }

    @Override
    public void init(@NonNull VideoManager mgr, @NonNull CustomPlaybackOverlayFragment fragment) {
        super.init(mgr, fragment);
        mDanmakuView = fragment.binding.danmuView;
        mDanmakuView.showFPS(customerUserPreferences.isDanmuFps());
        mDanmaKuShow = customerUserPreferences.isDanmuController();
        this.danmuApi = KoinJavaComponent.get(DanmuApi.class);
        initDanmaku();
//        DanmakuTimer.useOrigin = true;
    }

    @Override
    protected void doStartItem(BaseItemDto item, long position, StreamInfo response) {
        IDanmakuView danmakuView = getDanmakuView();
        if (danmakuView.isPrepared()) {
            danmakuView.stop();
        }
        if (position > 0) {
            setDanmakuStartSeekPosition(position);
        } else {
            setDanmakuStartSeekPosition(-1);
        }
        onPrepareDanmaku(this);
    }

    @Override
    public void pause() {
        super.pause();
        danmakuOnPause();
    }

    @Override
    public void play(long position) {
        boolean resume = mPlaybackState == PlaybackState.PAUSED;
        super.play(position);
        if (resume) {
            resume();
        }
    }

    public void resume() {
        danmakuOnResume();
    }

    @Override
    public void stop() {
        super.stop();
        getDanmakuView().removeAllDanmakus(true);
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        releaseDanmaku(this);
    }

    @Override
    public void playerErrorEncountered() {
        super.playerErrorEncountered();
        releaseDanmaku(this);
    }

    @Override
    public void seek(long pos, boolean skipToNext) {
        // 下一集不进行
        if (getDanmakuView() == null || !getDanmakuView().isPrepared()) {
            //如果没有初始化过的，记录位置等待
            setDanmakuStartSeekPosition(pos);
        } else {
            //如果已经初始化过的，直接seek到对于位置
            resolveDanmakuSeek(this, pos);
        }
        super.seek(pos, skipToNext);
    }

    protected void danmakuOnPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
    }

    protected void danmakuOnResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    private void initDanmaku() {
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, customerUserPreferences.getDanmuRows()); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        DanamakuAdapter danamakuAdapter = new DanamakuAdapter(mDanmakuView);
        mDanmakuContext = DanmakuContext.create();
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(customerUserPreferences.getDanmuSpeed())
                .setScaleTextSize(customerUserPreferences.getDanmuFontSize())
                .setCacheStuffer(new SpannedCacheStuffer(), danamakuAdapter) // 图文混排使用SpannedCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        if (mDanmakuView != null) {
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                    //倍速
//                    if (videoSpeed != 1.0f) {
////                        timer.update(mCurrentPosition);
//                        timer.add((long) (timer.lastInterval() * (videoSpeed - 1)));
//                    }
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                }

                @Override
                public void prepared() {
                    IDanmakuView danmakuView = getDanmakuView();
                    if (danmakuView != null) {
                        long danmakuStartSeekPosition = Math.max(getDanmakuStartSeekPosition(), mCurrentPosition);
                        if (danmakuStartSeekPosition > 0) {
                            danmakuView.start();
                            danmakuView.seekTo(danmakuStartSeekPosition);
                        } else {
                            danmakuView.start();
                        }
                        if (videoSpeed != 1.0f) {
                            danmakuView.setVideoSpeed(videoSpeed);
                        }
                        resolveDanmakuShow();
                    }
                }
            });
            mDanmakuView.enableDanmakuDrawingCache(true);
        }
    }

    private InputStream getIsStream() {
        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        if (currentlyPlayingItem == null) {
            return null;
        }

        Response<ByteReadChannel> danmuXmlFileById = null;
        try {
            danmuXmlFileById = danmuApi.getDanmuXmlFileById(currentlyPlayingItem.getId(), new HashSet<>());
        } catch (Exception e) {
            Context context = mFragment == null ? null : mFragment.getContext();
            InvalidStatusException invalidStatusException = null;
            if (e instanceof InvalidStatusException) {
                invalidStatusException = (InvalidStatusException) e;
            } else if (e.getCause() instanceof InvalidStatusException) {
                invalidStatusException = (InvalidStatusException) e.getCause();
            }
            if (invalidStatusException != null) {
                int status = invalidStatusException.getStatus();
                if (status >= 400 && status < 500) {
                    CustomerCommonUtils.show(context, "该视频没有弹幕");
                }
            } else {
                CustomerCommonUtils.show(context, "获取弹幕失败:" + e.getMessage());
            }
        }

        if (danmuXmlFileById == null) {
            return null;
        }

        danmuItem = currentlyPlayingItem;
        ByteReadChannel content = danmuXmlFileById.getContent();
        return new ByteReadChannelInputStream(content);
    }

    public boolean isShowDanmu() {
        return mDanmaKuShow;
    }

    public void changeDanmuShow(boolean show) {
        this.mDanmaKuShow = show;
        resolveDanmakuShow();
    }

    /**
     * 弹幕的显示与关闭
     */
    private void resolveDanmakuShow() {
        mHandler.post(() -> {
            if (mDanmaKuShow) {
                onPrepareDanmaku(DanmuPlaybackController.this);
                if (!getDanmakuView().isShown())
                    getDanmakuView().show();
            } else {
                if (getDanmakuView().isShown()) {
                    getDanmakuView().hide();
                }
            }
        });
    }

    /**
     * 开始播放弹幕
     */
    private void onPrepareDanmaku(DanmuPlaybackController gsyVideoPlayer) {
        if (!mDanmaKuShow) {
            // 未开启不加载
            return;
        }

        if (gsyVideoPlayer.getDanmakuView() != null && !gsyVideoPlayer.getDanmakuView().isPrepared() && gsyVideoPlayer.getParser() != null) {
            gsyVideoPlayer.getDanmakuView().prepare(gsyVideoPlayer.getParser(),
                    gsyVideoPlayer.getDanmakuContext());
        }
    }

    /**
     * 弹幕偏移
     */
    private void resolveDanmakuSeek(DanmuPlaybackController gsyVideoPlayer, long time) {
        if (gsyVideoPlayer.getDanmakuView() != null && gsyVideoPlayer.getDanmakuView().isPrepared()) {
            gsyVideoPlayer.getDanmakuView().seekTo(time);
        }
    }

    /**
     * 创建解析器对象，解析输入流
     *
     * @param stream
     * @return
     */
    private BaseDanmakuParser createParser(InputStream stream) {

        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }

        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }

    /**
     * 释放弹幕控件
     */
    private void releaseDanmaku(DanmuPlaybackController danmakuJellyfinGSYVideoPlayer) {
        if (danmakuJellyfinGSYVideoPlayer != null && danmakuJellyfinGSYVideoPlayer.getDanmakuView() != null) {
            danmakuJellyfinGSYVideoPlayer.getDanmakuView().release();
        }
    }

    public BaseDanmakuParser getParser() {
        BaseItemDto currentlyPlayingItem = getCurrentlyPlayingItem();
        if (mParser == null || currentlyPlayingItem != danmuItem) {
            mParser = createParser(getIsStream());
        }
        return mParser;
    }

    public DanmakuContext getDanmakuContext() {
        return mDanmakuContext;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        super.setPlaybackSpeed(speed);
        videoSpeed = speed;
        IDanmakuView danmakuView = getDanmakuView();
        if (danmakuView != null) {
            danmakuView.setVideoSpeed(videoSpeed);
        }
    }

    @Override
    protected void refreshCurrentPosition() {
        super.refreshCurrentPosition();
        if (customerUserPreferences.isDanmuFps()) {
            long currentTime = getDanmakuView().getCurrentTime();
            log.debug("当前弹幕时间: 视频={}, 弹幕={}, 时间差={}", mCurrentPosition, currentTime, (currentTime - mCurrentPosition));
        }
    }

    public IDanmakuView getDanmakuView() {
        return mDanmakuView;
    }

    public long getDanmakuStartSeekPosition() {
        return mDanmakuStartSeekPosition;
    }

    public void setDanmakuStartSeekPosition(long danmakuStartSeekPosition) {
        this.mDanmakuStartSeekPosition = danmakuStartSeekPosition;
    }

    public void setDanmaKuShow(boolean danmaKuShow) {
        mDanmaKuShow = danmaKuShow;
    }

    public boolean getDanmaKuShow() {
        return mDanmaKuShow;
    }
}
