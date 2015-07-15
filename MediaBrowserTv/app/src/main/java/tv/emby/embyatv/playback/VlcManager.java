package tv.emby.embyatv.playback;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.Map;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 7/11/2015.
 */
public class VlcManager implements IVideoPlayer {

    private PlaybackOverlayActivity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private FrameLayout mSurfaceFrame;
    private LibVLC mLibVLC;
    private String mCurrentVideoPath;
    private String mCurrentVideoMRL;
    private Media mCurrentMedia;
    private VlcEventHandler mHandler = new VlcEventHandler();
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    private long mForcedTime = -1;
    private long mLastTime = -1;


    private boolean mSurfaceReady = false;

    public VlcManager(PlaybackOverlayActivity activity, View view) {
        mActivity = activity;
        mSurfaceView = (SurfaceView) view.findViewById(R.id.player_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceFrame = (FrameLayout) view.findViewById(R.id.player_surface_frame);
        createPlayer();

    }

    public long getDuration() {
        return mLibVLC != null ? mLibVLC.getLength() : -1;
    }

    public long getCurrentPosition() {
        long time = mLibVLC.getTime();
        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mLibVLC.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        }
        return mForcedTime == -1 ? time : mForcedTime;
    }

    public boolean isPlaying() {
        return mLibVLC != null && mLibVLC.isPlaying();
    }

    public int getState() {
        return mLibVLC.getPlayerState();
    }

    public void start() {
        if (!mSurfaceReady) {
            TvApp.getApplication().getLogger().Error("Attempt to play before surface ready");
            return;
        }

        if (!mLibVLC.isPlaying()) {
            mLibVLC.playMRL(mCurrentVideoMRL);
        }

    }

    public void play() {
        mLibVLC.play();
        mSurfaceView.setKeepScreenOn(true);
    }

    public void pause() {
        mLibVLC.pause();
        mSurfaceView.setKeepScreenOn(false);

    }

    public void setPlaySpeed(float speed) {
        mLibVLC.setRate(speed);
    }

    public void stopPlayback() {
        mLibVLC.stop();
    }

    public void seekTo(long pos) {
        if (mLibVLC == null) return;
        mForcedTime = pos;
        mLastTime = mLibVLC.getTime();

        mLibVLC.setTime(pos);
    }

    public void setVideoPath(String path) {
        mCurrentVideoPath = path;
        mSurfaceHolder.setKeepScreenOn(true);

        //changeSurfaceLayout(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);

        mCurrentMedia = new Media(mLibVLC, path);
        mCurrentMedia.parse();
        mCurrentMedia.release();
        mCurrentVideoMRL = mCurrentMedia.getMrl();

    }

    public void setSubtitleTrack(int id) {
        mLibVLC.setSpuTrack(id);

    }

    public int getAudioTrack() {
        return mLibVLC.getAudioTrack();
    }

    public void setAudioTrack(int id) {
        mLibVLC.setAudioTrack(id);
    }

    public Map<Integer, String> getSubtitleTracks() {
        return mLibVLC.getSpuTrackDescription();
    }

    public void destroy() {
        releasePlayer();
    }

    private void createPlayer() {
        try {

            // Create a new media player
            mLibVLC = new LibVLC();
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    TvApp.getApplication().getLogger().Error("Error in LibVLC");
                }
            });

            TvApp.getApplication().getLogger().Debug("Hardware acceleration mode: "
                    + Integer.toString(mLibVLC.getHardwareAcceleration()));

            mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
//            mLibVLC.setDeblocking(-1);
            mLibVLC.setDevHardwareDecoder(LibVLC.DEV_HW_DECODER_AUTOMATIC);
            mLibVLC.setNetworkCaching(30000);
//
            mLibVLC.setVout(LibVLC.VOUT_ANDROID_SURFACE);
//            mLibVLC.setSubtitlesEncoding("");
            mLibVLC.setAout(LibVLC.AOUT_AUDIOTRACK);

            mLibVLC.setTimeStretching(false);
            mLibVLC.setVerboseMode(false);
            mLibVLC.setHdmiAudioEnabled(true); //TODO: figure out how to know this

            mLibVLC.init(TvApp.getApplication());
            mSurfaceHolder.addCallback(mSurfaceCallback);
            EventHandler.getInstance().addHandler(mHandler);

        } catch (Exception e) {
            TvApp.getApplication().getLogger().ErrorException("Error creating VLC player", e);
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
        }
    }

    private void releasePlayer() {
        if (mLibVLC == null)
            return;
        EventHandler.getInstance().removeHandler(mHandler);
        mLibVLC.stop();
        mLibVLC.detachSurface();
        mLibVLC = null;
        mSurfaceView.setKeepScreenOn(false);

    }

    private void changeSurfaceLayout(int videoWidth, int videoHeight, int videoVisibleWidth, int videoVisibleHeight, int sarNum, int sarDen) {
        int sw;
        int sh;

        // get screen size
        Activity activity = TvApp.getApplication().getCurrentActivity();
        if (activity == null) return; //called during destroy
        sw = activity.getWindow().getDecorView().getWidth();
        sh = activity.getWindow().getDecorView().getHeight();

        if (mLibVLC != null && !mLibVLC.useCompatSurface())
            mLibVLC.setWindowSize(sw, sh);

        double dw = sw, dh = sh;
        boolean isPortrait;

        isPortrait = mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // sanity check
        if (dw * dh == 0 || videoWidth * videoHeight == 0) {
            TvApp.getApplication().getLogger().Error("Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        if (sarDen == sarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = videoVisibleWidth;
            ar = (double)videoVisibleWidth / (double)videoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = videoVisibleWidth * (double)sarNum / sarDen;
            ar = vw / videoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        if (dar < ar)
            dh = dw / ar;
        else
            dw = dh * ar;


        // set display size
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width  = (int) Math.ceil(dw * videoWidth / videoVisibleWidth);
        lp.height = (int) Math.ceil(dh * videoHeight / videoVisibleHeight);
        mSurfaceView.setLayoutParams(lp);
        //subtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        if (mSurfaceFrame != null) {
            lp = mSurfaceFrame.getLayoutParams();
            lp.width = (int) Math.floor(dw);
            lp.height = (int) Math.floor(dh);
            mSurfaceFrame.setLayoutParams(lp);

        }

        mSurfaceView.invalidate();
//        subtitlesSurface.invalidate();
    }

    public void setOnErrorListener(PlaybackListener listener) {
        mHandler.setOnErrorListener(listener);
    }

    public void setOnCompletionListener(PlaybackListener listener) {
        mHandler.setOnCompletionListener(listener);
    }

    public void setOnPreparedListener(PlaybackListener listener) {
        mHandler.setOnPreparedListener(listener);
    }

    public void setOnProgressListener(PlaybackListener listener) {
        mHandler.setOnProgressListener(listener);
    }

    public void setOnSeekCompleteListener(MediaPlayer mp, MediaPlayer.OnSeekCompleteListener listener) {

    }

    private Surface mSurface;
    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mLibVLC != null) {
                final Surface newSurface = holder.getSurface();
                if (mSurface != newSurface) {
                    mSurface = newSurface;
                    mLibVLC.attachSurface(mSurface, VlcManager.this);
                    TvApp.getApplication().getLogger().Debug("Surface attached");
                    mSurfaceReady = true;
                }
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mLibVLC != null) mLibVLC.detachSurface();
            mSurfaceReady = false;

        }
    };

    @Override
    public void setSurfaceLayout(int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visibleHeight;
        mVideoVisibleWidth  = visibleWidth;
        mSarNum = sarNum;
        mSarDen = sarDen;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeSurfaceLayout(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);
            }
        });

    }

    @Override
    public int configureSurface(Surface surface, int i, int i1, int i2) {
        return -1;
    }

    @Override
    public void eventHardwareAccelerationError() {
        TvApp.getApplication().getLogger().Error("Hardware Acceleration Error");
        Utils.showToast(mActivity, "Hardware Acceleration Error");
    }
}
