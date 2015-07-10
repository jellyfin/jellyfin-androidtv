package tv.emby.embyatv.playback;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 6/13/2015.
 */
public class VlcVideoView extends SurfaceView implements IVideoView, IVideoPlayer {

    private PlaybackOverlayActivity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
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

    private boolean mSurfaceReady = false;

    public VlcVideoView(Context context) {
        super(context);
        init();
    }

    public VlcVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VlcVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            mSurfaceView = this;
            mSurfaceHolder = getHolder();
            createPlayer();
            
        }
    }

    @Override
    public int getDuration() {
        return mLibVLC != null ? ((Long) mLibVLC.getLength()).intValue() : -1;
    }

    @Override
    public int getCurrentPosition() {
        return mLibVLC != null ? ((Float) mLibVLC.getPosition()).intValue() : -1;
    }

    @Override
    public boolean isPlaying() {
        return mLibVLC != null && mLibVLC.isPlaying();
    }

    @Override
    public void start() {
        if (!mSurfaceReady) {
            TvApp.getApplication().getLogger().Error("Attempt to play before surface ready");
            return;
        }

        if (!mLibVLC.isPlaying()) {
            String[] options = mLibVLC.getMediaOptions(0);
            mLibVLC.playMRL(mCurrentVideoMRL, options);
        }

    }

    @Override
    public void pause() {

    }

    @Override
    public void stopPlayback() {
        releasePlayer();
    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public void setVideoPath(String path) {
        mCurrentVideoPath = path;
        mSurfaceHolder.setKeepScreenOn(true);

        //changeSurfaceLayout(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen);

        mCurrentMedia = new Media(mLibVLC, path);
        mCurrentMedia.parse();
        mCurrentMedia.release();
        mCurrentVideoMRL = mCurrentMedia.getMrl();

    }

    @Override
    public void onActivityCreated(PlaybackOverlayActivity activity) {
        mActivity = activity;
    }

    private void createPlayer() {
        try {

            // Create a new media player
            mLibVLC = new LibVLC();
            mLibVLC.init(getContext());
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    TvApp.getApplication().getLogger().Error("Error in LibVLC");
                }
            });

            TvApp.getApplication().getLogger().Debug("Hardware acceleration mode: "
                    + Integer.toString(mLibVLC.getHardwareAcceleration()));

            mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
            mLibVLC.setDevHardwareDecoder(-1);
            mLibVLC.setNetworkCaching(60000);

            mLibVLC.setVout(-1);
            mLibVLC.setSubtitlesEncoding("");
            mLibVLC.setAout(LibVLC.AOUT_OPENSLES);
            mLibVLC.setTimeStretching(true);
            mLibVLC.setVerboseMode(true);
//            mLibVLC.setHdmiAudioEnabled(true); //TODO: figure out how to know this

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
        mSurfaceHolder.setKeepScreenOn(false);

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

        isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

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
//        lp = surfaceFrame.getLayoutParams();
//        lp.width = (int) Math.floor(dw);
//        lp.height = (int) Math.floor(dh);
//        surfaceFrame.setLayoutParams(lp);

        mSurfaceView.invalidate();
//        subtitlesSurface.invalidate();
    }


    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        mHandler.setOnErrorListener(listener);
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        mHandler.setOnCompletionListener(listener);
    }

    @Override
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        mHandler.setOnPreparedListener(listener);
    }

    @Override
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
                    mLibVLC.attachSurface(mSurface, VlcVideoView.this);
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
        Utils.showToast(getContext(), "Hardware Acceleration Error");
    }
}
