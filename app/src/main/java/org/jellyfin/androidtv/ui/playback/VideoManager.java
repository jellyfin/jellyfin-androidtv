package org.jellyfin.androidtv.ui.playback;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.TvApp;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.apiclient.model.dto.MediaSourceInfo;
import org.jellyfin.apiclient.model.entities.MediaStream;
import org.jellyfin.apiclient.model.entities.MediaStreamType;
import org.koin.java.KoinJavaComponent;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class VideoManager implements IVLCVout.OnNewVideoLayoutListener {
    public final static int ZOOM_FIT = 0;
    public final static int ZOOM_AUTO_CROP = 1;
    public final static int ZOOM_STRETCH = 2;

    private int mZoomMode = ZOOM_FIT;

    private PlaybackOverlayActivity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mSubtitlesSurface;
    private FrameLayout mSurfaceFrame;
    private SimpleExoPlayer mExoPlayer;
    private PlayerView mExoPlayerView;
    private AspectRatioFrameLayout mAspectRatioFrameLayout;
    private LibVLC mLibVLC;
    private org.videolan.libvlc.MediaPlayer mVlcPlayer;
    private Media mCurrentMedia;
    private VlcEventHandler mVlcHandler = new VlcEventHandler();
    private Handler mHandler = new Handler();
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    private long mForcedTime = -1;
    private long mLastTime = -1;
    private long mMetaDuration = -1;
    private long mMetaVLCStreamStartPosition = -1;
    private long lastExoPlayerPosition = -1;

    private boolean nativeMode = false;
    private boolean mSurfaceReady = false;
    public boolean isContracted = false;
    private PlaybackListener errorListener;
    private PlaybackListener completionListener;
    private PlaybackListener preparedListener;

    public VideoManager(PlaybackOverlayActivity activity, View view) {
        mActivity = activity;
        mSurfaceView = view.findViewById(R.id.player_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceCallback);
        mSurfaceFrame = view.findViewById(R.id.player_surface_frame);
        mSubtitlesSurface = view.findViewById(R.id.subtitles_surface);
        mSubtitlesSurface.setZOrderMediaOverlay(true);
        mSubtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        mExoPlayer = new SimpleExoPlayer.Builder(TvApp.getApplication(), new DefaultRenderersFactory(TvApp.getApplication()) {
            @Override
            protected void buildTextRenderers(Context context, TextOutput output, Looper outputLooper, int extensionRendererMode, ArrayList<Renderer> out) {
                // Do not add text renderers since we handle subtitles
            }
        }).build();


        mExoPlayerView = view.findViewById(R.id.exoPlayerView);
        mExoPlayerView.setPlayer(mExoPlayer);
        mExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(@NonNull ExoPlaybackException error) {
                Timber.e("***** Got error from player");
                if (errorListener != null) errorListener.onEvent();
                stopProgressLoop();
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                // Do not call listener when paused
                if (playbackState == Player.STATE_READY && playWhenReady) {
                    if (preparedListener != null) preparedListener.onEvent();
                    startProgressLoop();
                } else if (playbackState == Player.STATE_ENDED) {
                    if (completionListener != null) completionListener.onEvent();
                    stopProgressLoop();
                }
            }
        });
    }

    public void init(int buffer, boolean isInterlaced) {
        createPlayer(buffer, isInterlaced);
    }

    public void setNativeMode(boolean value) {
        nativeMode = value;
        if (nativeMode) {
            mExoPlayerView.setVisibility(View.VISIBLE);
        } else {
            mExoPlayerView.setVisibility(View.GONE);
        }
    }

    public boolean isNativeMode() { return nativeMode; }
    public int getZoomMode() { return mZoomMode; }

    public void setZoom(int mode) {
        mZoomMode = mode;
        switch (mode) {
            case ZOOM_FIT:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_FIT);
                break;
            case ZOOM_AUTO_CROP:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                break;
            case ZOOM_STRETCH:
                mExoPlayerView.setResizeMode(mAspectRatioFrameLayout.RESIZE_MODE_FILL);
                break;
        }
    }

    // set by playbackController when a new vlc transcode stream starts, and before seeking
    public void setMetaVLCStreamStartPosition(long value) {
        mMetaVLCStreamStartPosition = value;
    }

    public long getMetaVLCStreamStartPosition() {
        return mMetaVLCStreamStartPosition;
    }

    public void setMetaDuration(long duration) {
        mMetaDuration = duration;
    }

    public long getDuration() {
        if (nativeMode){
            return mExoPlayer.getDuration() > 0 ? mExoPlayer.getDuration() : mMetaDuration;
        } else {
            return mVlcPlayer.getLength() > 0 ? mVlcPlayer.getLength() : mMetaDuration;
        }
    }

    public long getCurrentPosition() {
        if (nativeMode) {
            if (mExoPlayer == null) {
                return lastExoPlayerPosition;
            } else {
                long mExoPlayerCurrentPosition = mExoPlayer.getCurrentPosition();
                lastExoPlayerPosition = mExoPlayerCurrentPosition;
                return mExoPlayerCurrentPosition;
            }
        }

        if (mVlcPlayer == null) return 0;

        long time = mVlcPlayer.getTime();

        // vlc returns ms from stream start. metaStartPosition + time = actual position
        time = mMetaVLCStreamStartPosition != -1 ? time + getMetaVLCStreamStartPosition() : time;
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
        return nativeMode ? mExoPlayer.isPlaying() : mVlcPlayer != null && mVlcPlayer.isPlaying();
    }

    public void start() {
        if (nativeMode) {
            if (mExoPlayer == null) {
                Timber.e("mExoPlayer should not be null!!");
                mActivity.finish();
                return;
            }
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayerView.setKeepScreenOn(true);
            normalWidth = mExoPlayerView.getLayoutParams().width;
            normalHeight = mExoPlayerView.getLayoutParams().height;
        } else {
            if (!mSurfaceReady) {
                Timber.e("Attempt to play before surface ready");
                return;
            }

            if (!mVlcPlayer.isPlaying()) {
                mVlcPlayer.play();
            }
        }
    }

    public void play() {
        if (nativeMode) {
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayerView.setKeepScreenOn(true);
        } else {
            mVlcPlayer.play();
            mSurfaceView.setKeepScreenOn(true);
        }
    }

    public void pause() {
        if (nativeMode) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayerView.setKeepScreenOn(false);
        } else {
            mVlcPlayer.pause();
            mSurfaceView.setKeepScreenOn(false);
        }
    }

    public void stopPlayback() {
        if (nativeMode) {
            mExoPlayer.stop();
        } else {
            mVlcPlayer.stop();
        }
        stopProgressLoop();
    }

    public long seekTo(long pos) {
        if (nativeMode) {
            Long intPos = pos;
            Timber.i("Exo length in seek is: %d", mExoPlayer.getDuration());
            mExoPlayer.seekTo(intPos.intValue());
            return pos;
        } else {
            if (mVlcPlayer == null || !mVlcPlayer.isSeekable()) return -1;
            mForcedTime = pos;
            mLastTime = mVlcPlayer.getTime();
            Timber.i("VLC length in seek is: %d", mVlcPlayer.getLength());
            try {
                if (getDuration() > 0) mVlcPlayer.setPosition((float)pos / getDuration()); else mVlcPlayer.setTime(pos);

                return pos;

            } catch (Exception e) {
                Timber.e(e, "Error seeking in VLC");
                Utils.showToast(mActivity,  mActivity.getString(R.string.seek_error));
                return -1;
            }
        }
    }

    public void setVideoPath(@Nullable String path) {
        if (path == null) {
            Timber.w("Video path is null cannot continue");
            return;
        }
        Timber.i("Video path set to: %s", path);

        if (nativeMode) {
            try {
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(TvApp.getApplication(), "ATV/ExoPlayer");

                mExoPlayer.prepare(new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(path)));
            } catch (IllegalStateException e) {
                Timber.e(e, "Unable to set video path.  Probably backing out.");
            }
        } else {
            mSurfaceHolder.setKeepScreenOn(true);

            mCurrentMedia = new Media(mLibVLC, Uri.parse(path));
            mCurrentMedia.parse();
            mVlcPlayer.setMedia(mCurrentMedia);

            mCurrentMedia.release();
        }
    }

    public void disableSubs() {
        if (!nativeMode && mVlcPlayer != null) mVlcPlayer.setSpuTrack(-1);
    }

    public boolean setSubtitleTrack(int index, @Nullable List<MediaStream> allStreams) {
        if (!nativeMode && allStreams != null) {
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
                Timber.e("Could not locate subtitle with index %s in vlc track info", index);
                return false;
            } catch (NullPointerException e){
                Timber.e("No subtitle tracks found in player trying to set subtitle with index %s in vlc track info", index);
                return false;
            }

            Timber.i("Setting Vlc sub to %s", vlcSub.name);
            return mVlcPlayer.setSpuTrack(vlcSub.id);

        }

        return false;
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
                Timber.e("Could not locate audio with index %s in vlc track info", ndx);
                mVlcPlayer.setAudioTrack(ndx);
                return;
            } catch (NullPointerException e){
                Timber.e("No subtitle tracks found in player trying to set subtitle with index %s in vlc track info", ndx);
                mVlcPlayer.setAudioTrack(vlcIndex);
                return;
            }
            //debug
            Timber.d("Setting VLC audio track index to: %d / %d", vlcIndex, vlcTrack.id);
            for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
                Timber.d("VLC Audio Track: %s / %d", track.name, track.id);
            }
            //
            if (mVlcPlayer.setAudioTrack(vlcTrack.id)) {
                Timber.i("Setting by ID was successful");
            } else {
                Timber.i("Setting by ID not succesful, trying index");
                mVlcPlayer.setAudioTrack(vlcIndex);
            }
        } else {
            Timber.e("Cannot set audio track in native mode");
        }
    }

    public void setAudioDelay(long value) {
        if (!nativeMode && mVlcPlayer != null) {
            if (!mVlcPlayer.setAudioDelay(value * 1000)) {
                Timber.e("Error setting audio delay");
            } else {
                Timber.i("Audio delay set to %d", value);
            }
        }
    }

    public long getAudioDelay() {
        return mVlcPlayer != null ? mVlcPlayer.getAudioDelay() / 1000 : 0;
    }

    public void setCompatibleAudio() {
        if (!nativeMode) {
            mVlcPlayer.setAudioOutput("opensles_android");
            mVlcPlayer.setAudioOutputDevice("hdmi");
        }
    }

    public void setAudioMode() {
        if (!nativeMode) {
            setVlcAudioOptions();
        }
    }

    private void setVlcAudioOptions() {
        if(!Utils.downMixAudio()) {
            mVlcPlayer.setAudioDigitalOutputEnabled(true);
        } else {
            setCompatibleAudio();
        }
    }

    public void setVideoTrack(MediaSourceInfo mediaSource) {
        if (!nativeMode && mediaSource != null && mediaSource.getMediaStreams() != null) {
            for (MediaStream stream : mediaSource.getMediaStreams()) {
                if (stream.getType() == MediaStreamType.Video && stream.getIndex() >= 0) {
                    Timber.d("Setting video index to: %d", stream.getIndex());
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
        try {
            // Create a new media player
            ArrayList<String> options = new ArrayList<>(20);
            options.add("--network-caching=" + buffer);
            options.add("--no-audio-time-stretch");
            options.add("--avcodec-skiploopfilter");
            options.add("1");
            options.add("--avcodec-skip-frame");
            options.add("0");
            options.add("--avcodec-skip-idct");
            options.add("0");
            options.add("--android-display-chroma");
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
            options.add("--audio-desync");
            options.add(String.valueOf(KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getLibVLCAudioDelay())));
            options.add("-v");
            options.add("--vout=android-opaque,android-display");

            mLibVLC = new LibVLC(TvApp.getApplication(), options);
            Timber.i("Network buffer set to %d", buffer);

            mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
            setVlcAudioOptions();

            mSurfaceHolder.addCallback(mSurfaceCallback);
            mVlcPlayer.setEventListener(mVlcHandler);

            //setup surface
            mVlcPlayer.getVLCVout().detachViews();
            mVlcPlayer.getVLCVout().setVideoView(mSurfaceView);
            mVlcPlayer.getVLCVout().setSubtitlesView(mSubtitlesSurface);
            mVlcPlayer.getVLCVout().attachViews(this);
            Timber.d("Surface attached");
            mSurfaceReady = true;
        } catch (Exception e) {
            Timber.e(e, "Error creating VLC player");
            Utils.showToast(TvApp.getApplication(), TvApp.getApplication().getString(R.string.msg_video_playback_error));
        }
    }

    private void releasePlayer() {
        if (mVlcPlayer != null) {
            mVlcPlayer.setEventListener(null);
            mVlcPlayer.stop();
            mVlcPlayer.getVLCVout().detachViews();
            mVlcPlayer.release();
            mLibVLC.release();
            mLibVLC = null;
            mVlcPlayer = null;
        }

        if (mExoPlayer != null) {
            mExoPlayerView.setPlayer(null);
            mExoPlayer.release();
            mExoPlayer = null;
        }

        mSurfaceView.setKeepScreenOn(false);
    }

    int normalWidth;
    int normalHeight;

    public void contractVideo(int height) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mExoPlayerView.getLayoutParams() : mSurfaceView.getLayoutParams());
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
            mExoPlayerView.setLayoutParams(lp);
            mExoPlayerView.invalidate();
        } else mSurfaceView.setLayoutParams(lp);

        isContracted = true;

    }

    public void setVideoFullSize(boolean force) {
        if (normalHeight == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) (nativeMode ? mExoPlayerView.getLayoutParams() : mSurfaceView.getLayoutParams());
        if (force) {
            lp.height = -1;
            lp.width = -1;
        } else {
            lp.height = normalHeight;
            lp.width = normalWidth;
        }
        if (nativeMode) {
            lp.rightMargin = 0;
            lp.bottomMargin = 0;
            mExoPlayerView.setLayoutParams(lp);
            mExoPlayerView.invalidate();
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
            Timber.e("Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar;
        if (sarDen == sarNum) {
            /* No indication about the density, assuming 1:1 */
            ar = (double)videoVisibleWidth / (double)videoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            double vw = videoVisibleWidth * (double)sarNum / sarDen;
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
        mSubtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        if (mSurfaceFrame != null) {
            lp = mSurfaceFrame.getLayoutParams();
            lp.width = (int) Math.floor(dw);
            lp.height = (int) Math.floor(dh);
            mSurfaceFrame.setLayoutParams(lp);

        }

        Timber.d("Surface sized %d x %d ", lp.width, lp.height);
        mSurfaceView.invalidate();
        mSubtitlesSurface.invalidate();
    }

    public void setOnErrorListener(final PlaybackListener listener) {
        mVlcHandler.setOnErrorListener(listener);
        errorListener = listener;
    }

    public void setOnCompletionListener(final PlaybackListener listener) {
        completionListener = listener;
        mVlcHandler.setOnCompletionListener(listener);
    }

    public void setOnPreparedListener(final PlaybackListener listener) {
        preparedListener = listener;

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
    public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
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

    public Integer translateVlcAudioId(Integer vlcId) {
        Integer ourIndex = 0;
        for (org.videolan.libvlc.MediaPlayer.TrackDescription track : mVlcPlayer.getAudioTracks()) {
            if (track.id == vlcId) return ourIndex - 1; // Vlc has 'disabled' as first
            ourIndex++;
        }
        return ourIndex;
    }
}
