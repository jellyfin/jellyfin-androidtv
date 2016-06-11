package tv.emby.embyatv.playback;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.devbrackets.android.exomedia.EMVideoView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.List;

import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import tv.emby.embyatv.R;
import tv.emby.embyatv.TvApp;
import tv.emby.embyatv.util.Utils;

/**
 * Created by Eric on 7/11/2015.
 */
public class VideoManager implements IVLCVout.Callback {

    public final static int ZOOM_NORMAL = 0;
    public final static int ZOOM_VERTICAL = 1;
    public final static int ZOOM_HORIZONTAL = 2;
    public final static int ZOOM_FULL = 3;

    private int mZoomMode = ZOOM_NORMAL;

    private PlaybackOverlayActivity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mSubtitlesSurface;
    private FrameLayout mSurfaceFrame;
    private EMVideoView mVideoView;
    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private String mCurrentVideoPath;
    private String mCurrentVideoMRL;
    private Media mCurrentMedia;
    private VlcEventHandler mVlcHandler = new VlcEventHandler();
    private Handler mHandler = new Handler();
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    private int mCurrentBuffer;
    private boolean mIsInterlaced;

    private long mForcedTime = -1;
    private long mLastTime = -1;
    private long mMetaDuration = -1;

    private boolean nativeMode = false;
    private boolean mSurfaceReady = false;
    public boolean isContracted = false;
    private boolean hasSubtitlesSurface = false;

    public VideoManager(PlaybackOverlayActivity activity, View view) {
        mActivity = activity;
        mSurfaceView = (SurfaceView) view.findViewById(R.id.player_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceCallback);
        mSurfaceFrame = (FrameLayout) view.findViewById(R.id.player_surface_frame);
        mSubtitlesSurface = (SurfaceView) view.findViewById(R.id.subtitles_surface);
        if (Utils.is50()) {
            mSubtitlesSurface.setZOrderMediaOverlay(true);
            mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            hasSubtitlesSurface = true;
        } else {
            mSubtitlesSurface.setVisibility(View.GONE);
        }
        mVideoView = (EMVideoView) view.findViewById(R.id.videoView);

    }

    public void init(int buffer, boolean isInterlaced) {
        createPlayer(buffer, isInterlaced);
    }

    public void setNativeMode(boolean value) {
        nativeMode = value;
        if (nativeMode) {
            mVideoView.setVisibility(View.VISIBLE);
        } else {
            mVideoView.setVisibility(View.GONE);
        }
    }

    public boolean isNativeMode() { return nativeMode; }
    public int getZoomMode() { return mZoomMode; }

    public void setZoom(int mode) {
        mZoomMode = mode;
        switch (mode) {
            case ZOOM_NORMAL:
                mVideoView.setScaleY(1);
                mVideoView.setScaleX(1);
                break;
            case ZOOM_VERTICAL:
                mVideoView.setScaleX(1);
                mVideoView.setScaleY(1.33f);
                break;
            case ZOOM_HORIZONTAL:
                mVideoView.setScaleY(1);
                mVideoView.setScaleX(1.33f);
                break;
            case ZOOM_FULL:
                mVideoView.setScaleX(1.33f);
                mVideoView.setScaleY(1.33f);
                break;

        }
    }

    public void setMetaDuration(long duration) {
        mMetaDuration = duration;
    }

    public long getDuration() {
        if (nativeMode){
            return mVideoView.getDuration() > 0 ? mVideoView.getDuration() : mMetaDuration;
        } else {
            return mVlcPlayer.getLength() > 0 ? mVlcPlayer.getLength() : mMetaDuration;
        }
    }

    public long getCurrentPosition() {
        if (nativeMode) return mVideoView.getCurrentPosition();

        if (mVlcPlayer == null) return 0;

        long time = mVlcPlayer.getTime();
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
        return nativeMode ? mVideoView.isPlaying() : mVlcPlayer != null && mVlcPlayer.isPlaying();
    }

    public boolean canSeek() { return nativeMode || mVlcPlayer.isSeekable(); }

    public void start() {
        if (nativeMode) {
            mVideoView.start();
            mVideoView.setKeepScreenOn(true);
            normalWidth = mVideoView.getLayoutParams().width;
            normalHeight = mVideoView.getLayoutParams().height;
        } else {
            if (!mSurfaceReady) {
                TvApp.getApplication().getLogger().Error("Attempt to play before surface ready");
                return;
            }

            if (!mVlcPlayer.isPlaying()) {
                mVlcPlayer.play();
            }
        }

    }

    public void play() {
        if (nativeMode) {
            mVideoView.start();
            mVideoView.setKeepScreenOn(true);
        } else {
            mVlcPlayer.play();
            mSurfaceView.setKeepScreenOn(true);
        }
    }

    public void pause() {
        if (nativeMode) {
            mVideoView.pause();
            mVideoView.setKeepScreenOn(false);
        } else {
            mVlcPlayer.pause();
            mSurfaceView.setKeepScreenOn(false);
        }

    }

    public void setPlaySpeed(float speed) {
        if (!nativeMode) mVlcPlayer.setRate(speed);
    }

    public void stopPlayback() {
        if (nativeMode) {
            mVideoView.stopPlayback();
        } else {
            mVlcPlayer.stop();
        }
        stopProgressLoop();
    }

    public long seekTo(long pos) {
        if (nativeMode) {
            Long intPos = pos;
            mVideoView.seekTo(intPos.intValue());
            return pos;
        } else {
            if (mVlcPlayer == null || !mVlcPlayer.isSeekable()) return -1;
            mForcedTime = pos;
            mLastTime = mVlcPlayer.getTime();
            TvApp.getApplication().getLogger().Info("VLC length in seek is: " + mVlcPlayer.getLength());
            try {
                if (getDuration() > 0) mVlcPlayer.setPosition((float)pos / getDuration()); else mVlcPlayer.setTime(pos);

                return pos;

            } catch (Exception e) {
                TvApp.getApplication().getLogger().ErrorException("Error seeking in VLC", e);
                Utils.showToast(mActivity, "Unable to seek");
                return -1;
            }
        }
    }

    public void setVideoPath(String path) {
        mCurrentVideoPath = path;
        TvApp.getApplication().getLogger().Info("Video path set to: "+path);

        if (nativeMode) {
            try {
                mVideoView.setVideoPath(path);
            } catch (IllegalStateException e) {
                TvApp.getApplication().getLogger().ErrorException("Unable to set video path.  Probably backing out.", e);
            }
        } else {
            mSurfaceHolder.setKeepScreenOn(true);

            mCurrentMedia = new Media(mLibVLC, Uri.parse(path));
            mCurrentMedia.parse();
            mVlcPlayer.setMedia(mCurrentMedia);

            mCurrentMedia.release();
        }

    }

    public void hideSurface() {
        if (nativeMode) {
            mVideoView.setVisibility(View.INVISIBLE);
        } else {
            mSurfaceView.setVisibility(View.INVISIBLE);
        }
    }

    public void showSurface() {
        if (nativeMode) {
            mVideoView.setVisibility(View.VISIBLE);
        } else {
            mSurfaceView.setVisibility(View.VISIBLE);
        }
    }

    public void disableSubs() {
        if (!nativeMode && mVlcPlayer != null) mVlcPlayer.setSpuTrack(-1);
    }

    public boolean setSubtitleTrack(int index, List<MediaStream> allStreams) {
        if (!nativeMode) {
            //find the relative order of our sub index within the sub tracks in VLC
            int vlcIndex = 1; // start at 1 to account for "disabled"
            for (MediaStream stream : allStreams) {
                if (stream.getType() == MediaStreamType.Subtitle && !stream.getIsExternal()) {
                    if (stream.getIndex() == index) {
                        break;
                    }
                    vlcIndex++;
                }
            }

            org.videolan.libvlc.MediaPlayer.TrackDescription vlcSub;
            try {
                vlcSub = getSubtitleTracks()[vlcIndex];

            } catch (IndexOutOfBoundsException e) {
                TvApp.getApplication().getLogger().Error("Could not locate subtitle with index %s in vlc track info", index);
                return false;
            } catch (NullPointerException e){
                TvApp.getApplication().getLogger().Error("No subtitle tracks found in player trying to set subtitle with index %s in vlc track info", index);
                return false;
            }

            TvApp.getApplication().getLogger().Info("Setting Vlc sub to "+vlcSub.name);
            return mVlcPlayer.setSpuTrack(vlcSub.id);

        }

        return false;
    }

    public boolean addSubtitleTrack(String path) {
        return !nativeMode && mVlcPlayer.setSubtitleFile(path);
    }

    public int getAudioTrack() {
        return nativeMode ? -1 : mVlcPlayer.getAudioTrack();
    }

    public void setAudioTrack(int ndx, List<MediaStream> allStreams) {
        if (!nativeMode) {
            //find the relative order of our audio index within the audio tracks in VLC
            int vlcIndex = 1; // start at 1 to account for "disabled"
            for (MediaStream stream : allStreams) {
                if (stream.getType() == MediaStreamType.Audio && !stream.getIsExternal()) {
                    if (stream.getIndex() == ndx) {
                        break;
                    }
                    vlcIndex++;
                }
            }

            org.videolan.libvlc.MediaPlayer.TrackDescription vlcTrack;
            try {
                vlcTrack = mVlcPlayer.getAudioTracks()[vlcIndex];

            } catch (IndexOutOfBoundsException e) {
                TvApp.getApplication().getLogger().Error("Could not locate audio with index %s in vlc track info", ndx);
                mVlcPlayer.setAudioTrack(ndx);
                return;
            } catch (NullPointerException e){
                TvApp.getApplication().getLogger().Error("No subtitle tracks found in player trying to set subtitle with index %s in vlc track info", ndx);
                mVlcPlayer.setAudioTrack(vlcIndex);
                return;
            }
            //debug
            TvApp.getApplication().getLogger().Debug("Setting VLC audio track index to: "+vlcIndex + "/" + vlcTrack.id);
            for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
                TvApp.getApplication().getLogger().Debug("VLC Audio Track: "+track.name+"/"+track.id);
            }
            //
            if (mVlcPlayer.setAudioTrack(vlcTrack.id)) {
                TvApp.getApplication().getLogger().Info("Setting by ID was successful");
            } else {
                TvApp.getApplication().getLogger().Info("Setting by ID not succesful, trying index");
                mVlcPlayer.setAudioTrack(vlcIndex);
            }
        } else {
            TvApp.getApplication().getLogger().Error("Cannot set audio track in native mode");
        }
    }

    public void setAudioDelay(long value) {
        if (!nativeMode && mVlcPlayer != null) {
            if (!mVlcPlayer.setAudioDelay(value * 1000)) {
                TvApp.getApplication().getLogger().Error("Error setting audio delay");
            } else {
                TvApp.getApplication().getLogger().Info("Audio delay set to "+value);
            }
        }
    }

    public long getAudioDelay() { return mVlcPlayer != null ? mVlcPlayer.getAudioDelay() / 1000 : 0;}

    public void setCompatibleAudio() {
         if (!nativeMode) {
             mVlcPlayer.setAudioOutput("opensles_android");
             mVlcPlayer.setAudioOutputDevice("hdmi");
         }
    }

    public void setAudioMode() {
        if (!nativeMode) {
            mVlcPlayer.setAudioOutput(Utils.downMixAudio() ? "opensles_android" : "android_audiotrack");
            mVlcPlayer.setAudioOutputDevice("hdmi");
        }
    }

    public void setVideoTrack(MediaSourceInfo mediaSource) {
        if (!nativeMode && mediaSource != null && mediaSource.getMediaStreams() != null) {
            for (MediaStream stream : mediaSource.getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Video && stream.getIndex() >= 0) {
                    TvApp.getApplication().getLogger().Debug("Setting video index to: "+stream.getIndex());
                    mVlcPlayer.setVideoTrack(stream.getIndex());
                    return;
                }
            }
        }
    }

    public org.videolan.libvlc.MediaPlayer.TrackDescription[] getSubtitleTracks() {
        return nativeMode ? null : mVlcPlayer.getSpuTracks();
    }

    public void destroy() {
        releasePlayer();
    }

    private void createPlayer(int buffer, boolean isInterlaced) {
        if (mVlcPlayer != null && mIsInterlaced == isInterlaced && mCurrentBuffer == buffer) return; // don't need to re-create

        try {

            // Create a new media player
            ArrayList<String> options = new ArrayList<>(20);
            options.add("--network-caching=" + buffer);
            options.add("--no-audio-time-stretch");
            options.add("--avcodec-skiploopfilter");
            options.add("" + 1);
            options.add("--avcodec-skip-frame");
            options.add("0");
            options.add("--avcodec-skip-idct");
            options.add("0");
            options.add("--androidwindow-chroma");
            options.add("RV32");
            options.add("--audio-resampler");
            options.add("soxr");
            options.add("--stats");
            if (isInterlaced) {
                options.add("--video-filter=deinterlace");
                options.add("--deinterlace-mode=Bob");
            }
//            options.add("--subsdec-encoding");
//            options.add("Universal (UTF-8)");
            options.add("-v");

            mLibVLC = new LibVLC(options);
            TvApp.getApplication().getLogger().Info("Network buffer set to " + buffer);
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    new Exception().printStackTrace();
                    //todo custom error reporter
                    mActivity.finish();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            });

            mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
            mVlcPlayer.setAudioOutput(Utils.downMixAudio() ? "opensles_android" : "android_audiotrack");
            mVlcPlayer.setAudioOutputDevice("hdmi");


            mSurfaceHolder.addCallback(mSurfaceCallback);
            mVlcPlayer.setEventListener(mVlcHandler);

            //setup surface
            mVlcPlayer.getVLCVout().detachViews();
            mVlcPlayer.getVLCVout().setVideoView(mSurfaceView);
            if (hasSubtitlesSurface) mVlcPlayer.getVLCVout().setSubtitlesView(mSubtitlesSurface);
            mVlcPlayer.getVLCVout().attachViews();
            TvApp.getApplication().getLogger().Debug("Surface attached");
            mSurfaceReady = true;
            mVlcPlayer.getVLCVout().addCallback(this);


        } catch (Exception e) {
            TvApp.getApplication().getLogger().ErrorException("Error creating VLC player", e);
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
        }
    }

    private void releasePlayer() {
        if (mVlcPlayer == null) return;

        mVlcPlayer.setEventListener(null);
        mVlcPlayer.stop();
        mVlcPlayer.getVLCVout().detachViews();
        mVlcPlayer.release();
        mLibVLC = null;
        mVlcPlayer = null;
        mSurfaceView.setKeepScreenOn(false);

    }

    int normalWidth;
    int normalHeight;

    public void contractVideo(int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mVideoView.getLayoutParams() : mSurfaceView.getLayoutParams());
        if (isContracted) return;

        Activity activity = TvApp.getApplication().getCurrentActivity();
        int sw = activity.getWindow().getDecorView().getWidth();
        int sh = activity.getWindow().getDecorView().getHeight();
        float ar = (float)sw / sh;
        lp.height = height;
        lp.width = (int) Math.ceil(height * ar);
        lp.rightMargin = ((lp.width - normalWidth) / 2) - 110;
        lp.bottomMargin = ((lp.height - normalHeight) / 2) - 50;

        if (nativeMode) {
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        } else mSurfaceView.setLayoutParams(lp);

        isContracted = true;

    }

    public void setVideoFullSize() {
        if (normalHeight == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mVideoView.getLayoutParams() : mSurfaceView.getLayoutParams());
        lp.height = normalHeight;
        lp.width = normalWidth;
        if (nativeMode) {
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            mVideoView.setLayoutParams(lp);
            mVideoView.invalidate();
        } else mSurfaceView.setLayoutParams(lp);

        isContracted = false;

    }

    private void changeSurfaceLayout(int videoWidth, int videoHeight, int videoVisibleWidth, int videoVisibleHeight, int sarNum, int sarDen) {
        int sw;
        int sh;

        // get screen size
        Activity activity = TvApp.getApplication().getCurrentActivity();
        if (activity == null) return; //called during destroy
        sw = activity.getWindow().getDecorView().getWidth();
        sh = activity.getWindow().getDecorView().getHeight();

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
        normalWidth = lp.width;
        normalHeight = lp.height;
        mSurfaceView.setLayoutParams(lp);
        if (hasSubtitlesSurface) mSubtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        if (mSurfaceFrame != null) {
            lp = mSurfaceFrame.getLayoutParams();
            lp.width = (int) Math.floor(dw);
            lp.height = (int) Math.floor(dh);
            mSurfaceFrame.setLayoutParams(lp);

        }

        TvApp.getApplication().getLogger().Debug("Surface sized "+ lp.width+"x"+lp.height);
        mSurfaceView.invalidate();
        if (hasSubtitlesSurface) mSubtitlesSurface.invalidate();
    }

    public void setOnErrorListener(final PlaybackListener listener) {
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                TvApp.getApplication().getLogger().Error("***** Got error from player");
                listener.onEvent();
                stopProgressLoop();
                return true;
            }
        });

        mVlcHandler.setOnErrorListener(listener);
    }

    public void setOnCompletionListener(final PlaybackListener listener) {
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                listener.onEvent();
                stopProgressLoop();
            }
        });

        mVlcHandler.setOnCompletionListener(listener);
    }

    private MediaPlayer mNativeMediaPlayer;

    public void setOnPreparedListener(final PlaybackListener listener) {
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mNativeMediaPlayer = mp;
                listener.onEvent();
                startProgressLoop();
            }
        });

        mVlcHandler.setOnPreparedListener(listener);
    }

    public void setOnProgressListener(PlaybackListener listener) {
        progressListener = listener;
        mVlcHandler.setOnProgressListener(listener);
    }

    private PlaybackListener progressListener;
    private Runnable progressLoop;
    private void startProgressLoop() {
        progressLoop = new Runnable() {
            @Override
            public void run() {
                if (progressListener != null) progressListener.onEvent();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(progressLoop);
    }

    private void stopProgressLoop() {
        if (progressLoop != null) {
            mHandler.removeCallbacks(progressLoop);
        }
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mVlcPlayer != null) mVlcPlayer.getVLCVout().detachViews();
            mSurfaceReady = false;

        }
    };

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0 || isContracted)
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
    public void onSurfacesCreated(IVLCVout ivlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout ivlcVout) {
        TvApp.getApplication().getLogger().Error("VLC Hardware acceleration error");
    }

    public Integer translateVlcAudioId(Integer vlcId) {
        Integer ourIndex = 0;
        for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
            if (track.id == vlcId) return ourIndex - 1; // Vlc has 'disabled' as first
            ourIndex++;
        }
        return ourIndex;
    }
}
